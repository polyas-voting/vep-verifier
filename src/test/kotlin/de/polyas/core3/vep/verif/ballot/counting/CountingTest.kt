package de.polyas.core3.vep.verif.ballot.counting

import de.polyas.core3.vep.verif.ballot.ballotContent
import de.polyas.core3.vep.verif.ballot.decoding.DecodedBallot
import de.polyas.core3.vep.verif.ballot.decoding.DecodedCandidate
import de.polyas.core3.vep.verif.ballot.decoding.DecodedCandidateList
import de.polyas.core3.vep.verif.ballot.model.BallotId
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class CountingTest {

    @Test
    fun `simple ballot`() {
        val ballotConfigs: List<BallotSpec> = ballotWithTwoCandidates() // a simple ballot with two candidates

        val ballot1 = DecodedBallot(
            id = BallotId("A"),
            intentionallyInvalid = false,
            abstained = false,
            tooLong = false,
            decodedLists = listOf(
                DecodedCandidateList(
                    id = "A1",
                    votes = 0, // votes for the list
                    decodedCandidates = listOf(
                        DecodedCandidate(id = "A1-1", votes = 1, writeInContent = null)
                    )
                )
            )
        )
        val ballot2 = DecodedBallot(
            id =  BallotId("A"),
            intentionallyInvalid = false,
            abstained = false,
            tooLong = false,
            decodedLists = listOf(
                DecodedCandidateList(
                    id = "A1",
                    votes = 0, // votes for the list
                    decodedCandidates = listOf(
                        DecodedCandidate(id = "A1-1", votes = 1, writeInContent = null),
                        DecodedCandidate(id = "A1-2", votes = 1, writeInContent = null)
                    )
                )
            )
        )
        val decodedBallots = sequenceOf(ballot1, ballot2)

        val counting = Counting(ballotConfigs)
        val countingResult = counting.countDecodedBallots(decodedBallots)

        assertEquals(1, countingResult.size)
        val res = countingResult[0]
        assertEquals(1, res.valid)
        assertEquals(1, res.invalid)
    }

    @Test
    fun `invalid by too long flag`() {
        val ballotConfigs: List<BallotSpec> = ballotWithTwoCandidates() // a simple ballot with two candidates

        val ballot = DecodedBallot(
            id = BallotId("A"),
            intentionallyInvalid = false,
            abstained = false,
            tooLong = true, // invalid
            decodedLists = listOf(
                DecodedCandidateList(
                    id = "A1",
                    votes = 0, // votes for the list
                    decodedCandidates = listOf(
                        DecodedCandidate(id = "A1-1", votes = 1, writeInContent = null)
                    )
                )
            )
        )

        val decodedBallots = sequenceOf(ballot)

        val counting = Counting(ballotConfigs)
        val countingResult = counting.countDecodedBallots(decodedBallots)

        assertEquals(1, countingResult.size) // only one ballot
        val res = countingResult[0]
        assertEquals(1, res.invalid)   // one valid ballot
    }
}

////////////////////////////////////////

private fun ballotWithTwoCandidates(abstainOption: Boolean = false, invalidOption: Boolean = false, minVotesForBallot: Int = 0) =
    listOf(
        ballotContent("A", "BallotA") {
            minVotes = minVotesForBallot
            showAbstainOption = abstainOption
            showInvalid = invalidOption
            list("Title of the third list", "Header1", "Header2") {
                choice("cand-A1-1", "cand-A1-1")
                choice("cand-B1-2", "cand-A1-2")
            }
        }
    )
