package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.ElGamalEncoding
import de.polyas.core3.crypto.elgamal.MultiCiphertext
import de.polyas.core3.crypto.elgamal.VerificationResult.Correct
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.crypto.elgamal.zkp.DecryptionZKP
import de.polyas.core3.vep.verif.types.MessageWithProofPacket
import de.polyas.core3.vep.verif.types.MessageWithProofPacket.MessageWithProof
import de.polyas.core3.vep.verif.types.MixPacket
import de.polyas.core3.vep.verif.types.PublicKeyWithZKP

/**
 * Verification of the result of decryption
 */
object DecryptedPacketsVerif {
    private val group = EllipticCurveInst()

    fun verify(
        ek: PublicKeyWithZKP<ECElement>,
        mixPackets: () -> Sequence<MixPacket>,
        decryptedPackets: () -> Sequence<MessageWithProofPacket>
    ) {
        applyToCorresponding(mixPackets(), decryptedPackets()) { mixPacket, decPacket ->
            check(mixPacket.publicLabel == decPacket.publicLabel)

            val decryptionZKP = DecryptionZKP(group, ek.publicKey)

            applyToCorresponding(mixPacket.ciphertexts, decPacket.messagesWithZKP) { ciphertexts, decrypted ->
                verifyMultiCiphertextDecryption(decryptionZKP, ciphertexts, decrypted)
            }
        }
    }

    private fun <G> verifyMultiCiphertextDecryption(
        dec: DecryptionZKP<G>, ciphertexts: MultiCiphertext<G>, messageWithProof: MessageWithProof<G>
    ) {
        val n = ciphertexts.size()

        check (messageWithProof.proof.size == n) {
            "The given multi-ciphertext and zkp list are of different lengths: " +
                    "$n and ${messageWithProof.proof.size} respectively"
        }

        ciphertexts.ciphertexts.zip(messageWithProof.proof).forEach { (ciphertext, proof) ->
            val result = dec.verifyZkpOnly(ciphertext, proof)
            check (result is Correct) {
                "Incorrect zero-knowledge proof of decryption: $result"
            }
        }

        val expectedPlaintexts = List(n) { i: Int ->
            dec.applyDecryptionFactor(ciphertexts[i], messageWithProof.proof[i].decryptionShare)
        }
        val expectedMessage = ElGamalEncoding.decodeToMessage(expectedPlaintexts, group)
            .getOrElse { error("Cannot decode a message on the decryption board: ${it.message}") }

        check (messageWithProof.message == expectedMessage) {
            "A message on the decryption board does not have the expected value: " +
                    " given message = ${messageWithProof.message}, expected = $expectedMessage"
        }
    }
}
