package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.ElGamalEncoding
import de.polyas.core3.crypto.elgamal.MultiPlaintext
import de.polyas.core3.crypto.elgamal.VerificationResult
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.vep.verif.ballot.BallotDecoder
import de.polyas.core3.vep.verif.types.*
import de.polyas.core3.vep.verif.verification.BallotFlaggedVerif.ctSize
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
        ballotFlagged: () -> Sequence<Packet<AnnotatedBallot>>,
        revocationTokens: () -> Sequence<RevocationToken>?,
    ) {
        println("  *** INFO: the number of cast ballot packets = ${ballotBox().count()}. A packet may contain multiple ballots.")

        val revocationTokenSequence = revocationTokens()
        val revokedVoters: Set<String> =
            revocationTokenSequence
                ?.flatMap(RevocationToken::voterIds)
                ?.toSet()
                ?: emptySet()

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
            checkCorrespondingEntries(ballotEntry, flagged, votersById, ek, registry, revokedVoters, votersWhoVoted)
        }
    }

    private fun checkCorrespondingEntries(
        ballotEntry: BallotEntry,
        flagged: AnnotatedBallot,
        votersById: Map<String, Voter>,
        ek: PublicKeyWithZKP<ECElement>,
        registry: Registry,
        revokedVoters: Set<String>,
        votersWhoVoted: MutableSet<String>
    ) {
        val voterId = ballotEntry.ballot.reference
        val status = flagged.status
        val voter = votersById[voterId]

        check(flagged.ballot == ballotEntry) {
            "input entry for voter with reference $voterId not copied properly to the output entry"
        }

        if (voter == null) {
            check(status == AnnotatedBallot.Status.INCORRECT) {
                "Ballot of voter $voterId should be marked as incorrect: invalid voter identifier"
            }
            return
        }

        val expectedAnnotation =
            checkPublicLabel(ballotEntry, voter)
                .and { checkCiphertextLength(ballotEntry, registry) }
                .and { checkDuplicates(ballotEntry, votersWhoVoted) }
                .and { checkZkp(ek, ballotEntry, voter, ballotVerifier) }
                .and { checkRevocation(voter.id, revokedVoters) }

        when (expectedAnnotation) {
            is ExpectedAnnotation.OK ->
                check(status == AnnotatedBallot.Status.OK) {
                    "A correct ballot of voter $voterId not marked as valid"
                }
            is ExpectedAnnotation.Incorrect ->
                check(status == AnnotatedBallot.Status.INCORRECT) {
                    "Ballot of voter $voterId should be marked as incorrect: ${expectedAnnotation.reason}"
                }
            is ExpectedAnnotation.Revoked ->
                check(status == AnnotatedBallot.Status.REVOKED) {
                    "Ballot of voter $voterId should be marked as revoked"
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
    fun ctSize(noBytes: Int): Int {
        val mpt = MultiPlaintext(ElGamalEncoding.encodeString(join("", Collections.nCopies(noBytes, "x")), group))
        return mpt.size()
    }
}

private fun checkPublicLabel(ballotEntry: BallotEntry, voter: Voter): ExpectedAnnotation {
    val voterId = ballotEntry.ballot.reference

    return if (ballotEntry.ballot.publicLabel != voter.publicLabel) {
        ExpectedAnnotation.Incorrect("Ballot of voter $voterId has mismatched public label")
    } else {
        ExpectedAnnotation.OK
    }
}

private fun checkCiphertextLength(
    ballotEntry: BallotEntry,
    registry: Registry,
): ExpectedAnnotation {
    val ballot = ballotEntry.ballot

    val ballotDecoder = BallotDecoder(registry.ballotStructures)
    val expectedPlaintextSize = ballotDecoder.encodedPlaintextSize(ballot.publicLabel)
        ?: return ExpectedAnnotation.Incorrect( "The ballot has incorrect public label ${ballot.publicLabel}")

    val expectedCiphertextLength = ctSize(expectedPlaintextSize)
    if (ballot.encryptedChoice.ciphertexts.size != expectedCiphertextLength) {
        return ExpectedAnnotation.Incorrect("The ballot has wrong ciphertext length")
    }

    return ExpectedAnnotation.OK
}

private fun checkDuplicates(
    ballotEntry: BallotEntry,
    votersWhoVoted: MutableSet<String>
): ExpectedAnnotation {
    val voterId = ballotEntry.ballot.reference
    if (voterId in votersWhoVoted) {
        return ExpectedAnnotation.Incorrect("Duplicated ballot of voter $voterId")
    } else {
        votersWhoVoted += voterId
        return ExpectedAnnotation.OK
    }
}

private fun checkZkp(
    ek: PublicKeyWithZKP<ECElement>,
    ballotEntry: BallotEntry,
    voter: Voter,
    ballotVerifier: BallotVerifier<ECElement>
): ExpectedAnnotation {
    val zkpResult = ballotVerifier.verifyZKP(ek.publicKey, voter.cred, ballotEntry.ballot)
    return if (zkpResult is VerificationResult.Failed) {
        ExpectedAnnotation.Incorrect("invalid zero-knowledge proof: ${zkpResult.errorMessage}")
    } else {
        ExpectedAnnotation.OK
    }
}

private fun checkRevocation(
    voterId: String,
    revokedVoters: Set<String>
): ExpectedAnnotation {
    return if (voterId in revokedVoters) {
        ExpectedAnnotation.Revoked
    } else {
        ExpectedAnnotation.OK
    }
}

///

private sealed interface ExpectedAnnotation {
    data object OK : ExpectedAnnotation
    data object Revoked : ExpectedAnnotation
    data class Incorrect(val reason: String) : ExpectedAnnotation
}

private fun ExpectedAnnotation.and(other: () -> ExpectedAnnotation): ExpectedAnnotation =
    when (this) {
        ExpectedAnnotation.OK -> other()
        else -> this
    }



