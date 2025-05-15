package de.polyas.core3.vep.verif.crypto

import de.polyas.core3.crypto.elgamal.*
import de.polyas.core3.crypto.elgamal.VerificationResult.Correct
import de.polyas.core3.crypto.elgamal.VerificationResult.Failed
import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP
import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP.Challenger
import de.polyas.core3.crypto.annotation.Doc
import de.polyas.core3.crypto.std.*
import org.bouncycastle.util.encoders.Base64
import java.math.BigInteger

/**
 * Ballot algorithms (ballot creation and verification) for Core3 Verifiable EP.
 */
class VerifiableEPBallotAlgo<GroupElem>(private val group: CyclicGroup<GroupElem>) {

    data class Ballot<GroupElem>(
        val encryptedChoice: MultiCiphertext<GroupElem>,
        val zkp: List<DlogNIZKP.Proof>,
        val publicLabel: String,
        val reference: String,
        val signature: DlogNIZKP.Proof
    ) {
        /**
         * Returns normalized representation of this ballot as a message (byte
         * string). To be used in order to obtain an unambiguous representation
         * before hashing or signing.
         */
        fun asNormalizedByteString(group: CyclicGroup<GroupElem>): Message = buildMessage {
            put(encryptedChoice.ciphertexts.size)
            encryptedChoice.ciphertexts.forEach { (x, y) ->
                putWithLength(group.asBytes(x))
                putWithLength(group.asBytes(y))
            }

            putWithLength(publicLabel)
            putWithLength(reference)

            put(zkp.size)
            zkp.forEach { (c, f) ->
                putWithLength(c.toByteArray())
                putWithLength(f.toByteArray())
            }

            putWithLength(signature.c.toByteArray())
            putWithLength(signature.f.toByteArray())
        }
    }

    private val cryptoSystem = Cryptosystem(group)
    private val pedersen = Pedersen(group)

    /**
     * Creates an encrypted ballot. Used for tests only (in production, ballots are created on the client side).
     */
    fun createBallot(
        encryptionKey: GroupElem,
        voterId: String,
        publicLabel: String,
        votersChoice: MultiPlaintext,
        referenceCoin: BigInteger,
        privateCredential: BigInteger,
        encCoins: List<BigInteger> = List(votersChoice.size()) { SRNG.nextBigInt(group.order) },
    ): Ballot<GroupElem> {
        val publicCredential = group.powerOfG(privateCredential)
        val n = votersChoice.size()

        votersChoice.plaintexts.forEach { plaintext ->
            require(group.validPlaintext(plaintext)) { "Value $plaintext is not a valid plaintext" }
        }

        val ciphertext = MultiCiphertext(n) { i ->
            cryptoSystem.encrypt(encryptionKey, votersChoice[i], encCoins[i])
        }

        val ref = reference(voterId, referenceCoin)

        val dlogZKP = DlogNIZKP(group, ballotChallenger(encryptionKey, publicLabel, ref, ciphertext))
        val zkp = List(n) { i -> dlogZKP.createProof(ciphertext[i].x, encCoins[i]) }
        val signature = dlogZKP.createProof(publicCredential, privateCredential)

        return Ballot(ciphertext, zkp, publicLabel, ref, signature)
    }

    /**
     * Checks correctness of the zero-knowledge proofs and the signature
     * included in the [ballot].
     *
     * This check intended to be executed both by the election controller
     * before the ballot is published and by the universal verification procedure.
     *
     * @param publicKey The election (encryption) public key
     * @param ballot The ballot containing the encrypted choice, the required zero-knowledge proof and signature.
     */
    fun verifyZKP(publicKey: GroupElem, publicCredential: GroupElem, ballot: Ballot<GroupElem>): VerificationResult {

        if (ballot.zkp.size != ballot.encryptedChoice.size())
            return Failed("ZKP of knowledge of encryption coins has invalid length")

        val dlogZKP = DlogNIZKP(group, ballotChallenger(publicKey, ballot.publicLabel, ballot.reference, ballot.encryptedChoice))

        for (i in 0 until ballot.encryptedChoice.size()) {
            dlogZKP.verify(ballot.encryptedChoice[i].x, ballot.zkp[i]) onFailure { return it }
        }

        dlogZKP.verify(publicCredential, ballot.signature) onFailure { return Failed("Invalid signature: ${it.errorMessage}") }

        return Correct
    }

    /**
     * Checks the consistency of the given voter identifier [voterId]
     * with the given reference [ref], using [referenceCoin].
     *
     * This check is intended to be executed by the election controller
     * before the ballot is published ([voterId] and [referenceCoin] are only
     * available at this moment).
     */
    fun checkReferenceConsistency(
        voterId: String,
        ref: String,
        referenceCoin: BigInteger
    ): VerificationResult {
        val expectedRef = reference(voterId, referenceCoin)
        return if (ref == expectedRef) Correct
        else Failed("Invalid reference: given = $ref, expected = $expectedRef")
    }

    /**
     * Computes the reference from the given [voterId] and [referenceCoin],
     * as the (hash of the) Pedersen commitment to [voterId] with the randomness [referenceCoin].
     */
    fun reference(voterId: String, referenceCoin: BigInteger): String {
        val commitment = pedersen.commit(uniformHash(voterId), referenceCoin)
        val hash = Hashes.sha256(group.asBytes(commitment))
        return Base64.toBase64String(hash).take(22).chunked(6).joinToString("-")
    }

    private fun ballotChallenger(
        publicKey: GroupElem, publicLabel: String, ref: String, ciphertext: MultiCiphertext<GroupElem>
    ): Challenger<GroupElem> {

        val initial = initialDigestSha512 {
            digest(group.asBytes(group.generator))
            digest(group.asBytes(publicKey))
            digest(publicLabel)
            digest(ref)

            for ((x, y) in ciphertext.ciphertexts) {
                digest(group.asBytes(x))
                digest(group.asBytes(y))
            }
        }

        return Challenger { _: GroupElem, initialZKPMessage: GroupElem ->
            initial.continueUniformHash(group.order) {
                digest(group.asBytes(initialZKPMessage))
            }
        }
    }

    private fun uniformHash(s: String): BigInteger = uniformHash(group.order) {
        digest(s)
    }
}
