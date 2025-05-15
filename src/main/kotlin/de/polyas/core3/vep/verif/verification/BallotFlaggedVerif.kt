package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.ElGamalEncoding
import de.polyas.core3.crypto.elgamal.MultiPlaintext
import de.polyas.core3.crypto.elgamal.VerificationResult
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.vep.verif.ballot.BallotDecoder
import de.polyas.core3.vep.verif.types.*
import java.lang.String.join
import java.util.*

/**
 * Verification of the board ballot-flagged.
 *
 * This board should contain all the ballots from the ballot box, where
 * invalid ballots (for various reasons) are flagged as such and revoked
 * ballot (if the revocations are enabled) are marked as revoked.
 */
object BallotFlaggedVerif {

    private val group = EllipticCurveInst()
    private val ballotVerifier = BallotVerifier(group)

    fun verify(
        ek: PublicKeyWithZKP<ECElement>,
        registry: Registry,
        ballotBox: () -> Sequence<BallotPackage>,
        ballotFlagged: () -> Sequence<Packet<Annotated>>,
        revocationTokens: () -> Sequence<RevocationToken>?,
    ) {
        println("  *** INFO: the number of cast ballot packets = ${ballotBox().count()}. A packet may contain multiple ballots.")

        val revocationTokenSequence = revocationTokens()
        val revokedVoters: Set<String> =
            revocationTokenSequence?.flatMap(RevocationToken::voterIds)?.toSet() ?: emptySet()

        reportRevokedVoters(revocationTokenSequence, revokedVoters)

        // build an auxiliary mapping from voter identifiers to voters;
        // note that at this point there are no duplicated keys (as the registry has been verified)
        val votersById: Map<String, Voter> = registry.voters.associateBy(Voter::id)

        // we maintain the list of voters whose ballots have been processed so far (used to detect duplicates)
        val votersWhoVoted = mutableSetOf<String>()

        applyToCorresponding(
            ballotBox().flatMap { it.ballots },
            ballotFlagged().flatMap { it.values }
        ) { ballotEntry, flagged ->
            val ballot = ballotEntry.ballot
            val voterId = ballot.reference
            val status = flagged.status

            // 0. check that the output entry contains the input entry
            check(flagged.ballot == ballotEntry) {
                "input entry for voter with reference $voterId not copied properly to the output entry"
            }

            // 1. Check that the voter is registered
            val voter = votersById[voterId] // info record for the voter, taken from the registry
            if (voter == null) {
                check (status == Annotated.Status.INCORRECT) {
                    "Ballot with invalid voter reference $voterId not flagged as invalid"
                }
                return@applyToCorresponding
            }

            // 2 Check ballots with mismatched public labels (different from the one in the registry)
            if (ballot.publicLabel != voter.publicLabel) {
                check (status == Annotated.Status.INCORRECT) {
                    "Ballot of voter $voterId has mismatched public label, but it is not flagged as invalid"
                }
                return@applyToCorresponding
            }

            // 2. Compute the expected plaintext length and, doing this, check the label
            val ballotDecoder = BallotDecoder(registry.ballotStructures)
            val expectedPlaintextSize = ballotDecoder.encodedPlaintextSize(ballot.publicLabel) // null when the public label is incorrect
            if (expectedPlaintextSize == null) {
                check (status == Annotated.Status.INCORRECT) {
                    "Ballot of voter $voterId has incorrect public label ${ballot.publicLabel}, but it is not flagged as invalid"
                }
                return@applyToCorresponding
            }

            // 3. Check the expected ciphertext length
            val expectedCiphertextLength = ctSize(expectedPlaintextSize)
            if (ballot.encryptedChoice.ciphertexts.size != expectedCiphertextLength) {
                check (status == Annotated.Status.INCORRECT) {
                    "Ballot of voter $voterId has wrong ciphertext length, but it is not flagged as invalid"
                }
                return@applyToCorresponding
            }

            // 4. Check that duplicated ballots are flagged as invalid
            if (voterId in votersWhoVoted) {
                check(status == Annotated.Status.INCORRECT) {"duplicated ballot of voter $voterId not marked as invalid"}
                return@applyToCorresponding
            }
            votersWhoVoted += voterId

            // 5. Check that invalid ballots (which do not pass the cryptographic checks) are flagged as invalid
            val zkpResult = ballotVerifier.verifyZKP(ek.publicKey, voter.cred, ballot)
            if (zkpResult is VerificationResult.Failed) {
                check(status == Annotated.Status.INCORRECT) {
                    "Ballot of voter $voterId has wrong zero-knowledge proof, but it is not flagged as invalid"
                }
                return@applyToCorresponding
            }

            // 7. Check that revoked ballots are flagged as revoked
            if (voterId in revokedVoters) {
                check (status == Annotated.Status.REVOKED) {
                    "Ballot of a revoked voter $voterId not flagged as filtered out"
                }
                return@applyToCorresponding
            }

            check (status == Annotated.Status.OK) {
                "A correct ballot of voter $voterId not marked as valid"
            }
        }
    }

    private fun reportRevokedVoters(
        revocationTokenSequence: Sequence<RevocationToken>?,
        revokedVoters: Set<String>
    ) {
        if (revocationTokenSequence == null) {
            println("  *** INFO: no revocation board found")
        } else {
            println("  *** INFO: revocation board found")
            if (revokedVoters.isEmpty()) {
                println("  *** INFO: no revoked voters on the revocation board")
            } else {
                println("  *** INFO: voters with the following (anonymous) identifiers have been revoked:")
                revokedVoters.sorted().forEach {
                    println("      - $it")
                }
            }
        }
    }

    /** Computes the ciphertext size for the given plaintext length [noBytes] */
    private fun ctSize(noBytes: Int): Int {
        val mpt = MultiPlaintext(ElGamalEncoding.encodeString(join("", Collections.nCopies(noBytes, "x")), group))
        return mpt.size()
    }
}
