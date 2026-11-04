/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.ElGamalEncoding
import de.polyas.core3.crypto.elgamal.MultiPlaintext
import de.polyas.core3.crypto.elgamal.VerificationResult
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.vep.verif.ballot.decoding.BallotDecoder
import de.polyas.core3.vep.verif.types.*
import java.lang.String.join
import java.math.BigInteger
import java.util.*

/**
 * Verification of the board ballot-flagged.
 *
 * This board should contain all the ballots from the ballot box, where
 * invalid ballots (for various reasons) are flagged as such and revoked
 * ballot (if the revocations are enabled) are marked as revoked.
 */
class BallotFlaggedVerif(
    private val registry: Registry,
    private val anonymousRevocations: Boolean
) {
    private val group = EllipticCurveInst()
    private val ballotVerifier = BallotVerifier(group)
    private val ballotDecoder = BallotDecoder(registry.ballotStructures)

    fun verify(
        ek: PublicKeyWithZKP<ECElement>,
        ballotBox: () -> Sequence<BallotPackage>,
        ballotFlagged: () -> Sequence<Packet<AnnotatedBallot>>,
        revocationTokens: () -> Sequence<RevocationToken>?,
    ) {
        val revocationTokenSequence = revocationTokens()
        val revokedVoters: Set<String> =
            revocationTokenSequence
                ?.flatMap(RevocationToken::voterIds)
                ?.map(::voterIdFromRevocationVoterId)
                ?.toSet()
                ?: emptySet()

        reportRevokedVoters(revocationTokenSequence, revokedVoters)

        // build an auxiliary mapping from voter identifiers to voters;
        // we assume that at this point there are no duplicated keys (since the registry has been verified)
        val votersById: Map<String, Voter> = registry.voters.associateBy(Voter::id)

        // we maintain the list of voters whose ballots have been processed so far (used to detect duplicates)
        val votersWhoVoted = mutableSetOf<String>()

        applyToCorresponding(
            ballotBox().flatMap { it.ballots },
            ballotFlagged().flatMap { it.values }
        ) { ballotEntry, flagged ->
            checkCorrespondingEntries(ballotEntry, flagged, votersById, ek, revokedVoters, votersWhoVoted)
        }
    }

    private fun checkCorrespondingEntries(
        ballotEntry: BallotEntry,
        flagged: AnnotatedBallot,
        votersById: Map<String, Voter>,
        ek: PublicKeyWithZKP<ECElement>,
        revokedVoters: Set<String>,
        votersWhoVoted: MutableSet<String>
    ) {
        val voterId = ballotEntry.ballot.reference
        val status = flagged.status
        val voter = votersById[voterId]
        val ballot = ballotEntry.ballot

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
            checkPublicLabel(ballot, voter)
                .and { checkCiphertextLength(ballot) }
                .and { checkDuplicates(ballotEntry, votersWhoVoted) }
                .and { checkZkp(ek, ballotEntry, voter) }
                .and { checkRevocation(voter.id, revokedVoters) }

        votersWhoVoted += voterId

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
    private fun ciphertextSize(noBytes: Int): Int {
        val mpt = MultiPlaintext(ElGamalEncoding.encodeString(join("", Collections.nCopies(noBytes, "x")), group))
        return mpt.size()
    }

    private fun checkPublicLabel(ballot: Ballot<ECElement>, voter: Voter): ExpectedAnnotation {
        return if (ballot.publicLabel != voter.publicLabel) {
            ExpectedAnnotation.Incorrect("Ballot of voter ${ballot.reference} has mismatched public label")
        } else {
            ExpectedAnnotation.OK
        }
    }

    private fun checkCiphertextLength(ballot: Ballot<ECElement>): ExpectedAnnotation {
        val expectedPlaintextSize = ballotDecoder.encodedBallotSize(ballot.publicLabel)
            ?: return ExpectedAnnotation.Incorrect("The ballot has incorrect public label ${ballot.publicLabel}")
        val expectedCiphertextLength = ciphertextSize(expectedPlaintextSize)

        return if (ballot.encryptedChoice.ciphertexts.size != expectedCiphertextLength) {
            ExpectedAnnotation.Incorrect("The ballot has wrong ciphertext length")
        } else {
            ExpectedAnnotation.OK
        }
    }

    /**
     * Check duplicates, that it ballots with the same reference. It uses a mutable set
     * [votersWhoVoted], to determined which voters identifiers (references) have been already
     * used, and it mutates this set (adds new voter identifiers)
     *
     * @param ballotEntry Ballot entry to be checked for duplicates
     * @param votersWhoVoted Mutable set of voters who voted, modified by this function.
     */
    private fun checkDuplicates(
        ballotEntry: BallotEntry,
        votersWhoVoted: Set<String>
    ): ExpectedAnnotation {
        val voterId = ballotEntry.ballot.reference
        return if (voterId in votersWhoVoted) {
            ExpectedAnnotation.Incorrect("Duplicated ballot of voter $voterId")
        } else {
            ExpectedAnnotation.OK
        }
    }

    private fun checkZkp(
        ek: PublicKeyWithZKP<ECElement>,
        ballotEntry: BallotEntry,
        voter: Voter
    ): ExpectedAnnotation {
        return when (val zkpResult = ballotVerifier.verifyZKP(ek.publicKey, voter.cred, ballotEntry.ballot)) {
            is VerificationResult.Failed ->
                ExpectedAnnotation.Incorrect("invalid zero-knowledge proof: ${zkpResult.errorMessage}")
            else ->
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
    
    private fun voterIdFromRevocationVoterId(revocationVoterId: String): String =
        if (anonymousRevocations) revocationVoterId
        else AnonymousReferences.reference(revocationVoterId, BigInteger.ZERO) // deterministic references
}


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
