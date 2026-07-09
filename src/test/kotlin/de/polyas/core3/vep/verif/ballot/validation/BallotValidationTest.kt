package de.polyas.core3.vep.verif.ballot.validation

import de.polyas.core3.vep.verif.ballot.BallotChoiceCtx
import de.polyas.core3.vep.verif.ballot.ballotChoice
import de.polyas.core3.vep.verif.ballot.ballotContent
import de.polyas.core3.vep.verif.ballot.decoding.DecodedBallot
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import kotlin.test.Test
import kotlin.test.assertTrue

class BallotValidationTest {

    private val ballotSpec = ballotContent("A", "title") {
        maxVotes = 3
        list("list#1-1", "h1") {
            minVotesOnList = 1
            maxVotesOnList = 2
            choice("a") { maxVotes = 2 }
            choice("b") { writeInSize = 0 }
        }
        list("list#1-2", "h1") {
            maxVotesOnList = 1
            choice("c")
            choice("w") { writeInSize = 30 }
        }
    }

    private val ballotSpecNoInvalidOption = ballotSpec.copy(showInvalidOption = false)
    private val ballotSpecWithAbstainOption = ballotSpec.copy(showAbstainOption = true)

    ////

    @Test
    fun `missing write-in`() {
        val res = validateChoice(ballotSpec) {
            list(0) {
                !1
                !1
            }
            list {
                !0
                !1
            }
        }
        assertTrue(res is Validated.Malformed)
    }

    @Test
    fun `illegal write-in`() {
        val res = validateChoice(ballotSpec) {
            list(0) {
                !1
                !1
            }
            list {
                !(0 to "illegal")
                !(1 to "abcd")
            }
        }
        assertTrue(res is Validated.Malformed)
    }

    @Test
    fun `ballot is intentionally invalid`() {
        val res = validateChoice(ballotSpec) {
            list(0) {
                !0
                !1
            }
            list {
                !1
                !(0 to "abcde")
            }
            intentionallyInvalid = true
        }
        assertTrue(res is Validated.IntentionallyInvalid)
    }

    @Test
    fun `ballot is malformed intentionally invalid`() {
        val res = validateChoice(ballotSpecNoInvalidOption) {
            list(0) {
                !0
                !1
            }
            list {
                !1
                !(0 to "abcde")
            }
            intentionallyInvalid = true
        }
        assertTrue(res is Validated.Malformed)
    }

    @Test
    fun `ballot is malformed abstained`() {
        val res = validateChoice(ballotSpec) {
            list(0) {
                !0
                !1
            }
            list {
                !1
                !(0 to "abcde")
            }
            abstain = true
        }
        assertTrue(res is Validated.Malformed)
    }

    @Test
    fun `ballot is abstained`() {
        val res = validateChoice(ballotSpecWithAbstainOption) {
            list(0) {
                !0
                !1
            }
            list {
                !1
                !(0 to "abcde")
            }
            abstain = true
        }
        assertTrue(res is Validated.Abstained)
    }

    @Test
    fun `ballot is valid`() {
        val res = validateChoice(ballotSpec) {
            list(0) {
                !0
                !1
            }
            list {
                !1
                !(0 to "abcde")
            }
        }
        assertTrue(res is Validated.Valid)
    }

    ////

    private fun validateChoice(ballot: BallotSpec, block: BallotChoiceCtx.() -> Unit) : Validated<DecodedBallot> {
        return BallotValidator(ballot).validateBallot(ballotChoice(ballot, block))
    }
}
