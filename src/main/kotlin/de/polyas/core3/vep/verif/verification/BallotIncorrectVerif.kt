package de.polyas.core3.vep.verif.verification

import de.polyas.core3.vep.verif.types.AnnotatedBallot
import de.polyas.core3.vep.verif.types.Packet

/**
 * Verification of the board ballot-incorrect.
 *
 * This board should contain exactly one entry (of type String), for each entry flagged as invalid or filtered out.
 */
object BallotIncorrectVerif {

    fun verify(
        ballotFlagged: () -> Sequence<Packet<AnnotatedBallot>>,
        ballotIncorrect: () -> Sequence<String>
    ) {
        // we count and compare the number of flagged ballots and the number
        // of entries on 'ballotIncorrect'.

        val revokedCount: Int = ballotFlagged()
            .flatMap { it.values }
            .count { it.status == AnnotatedBallot.Status.REVOKED }
        val incorrectCount: Int = ballotFlagged()
            .flatMap { it.values }
            .count { it.status == AnnotatedBallot.Status.INCORRECT }
        val rejectedCount: Int = ballotIncorrect().count()
        val flaggedCount = revokedCount + incorrectCount

        require(flaggedCount == rejectedCount) {
            "The number of ballots flagged as invalid or revoked ($flaggedCount) not equal to the number of ballots published on ballotIncorrect ($rejectedCount)"}

        // reporting
        println("  *** INFO: the number of ballots flagged as revoked   = $revokedCount")
        val qualifier = if (incorrectCount == 0) "INFO" else "WARNING"
        println("  *** $qualifier: the number of ballots flagged as incorrect = $incorrectCount")
    }
}


