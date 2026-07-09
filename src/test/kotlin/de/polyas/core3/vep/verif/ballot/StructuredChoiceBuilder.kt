package de.polyas.core3.vep.verif.ballot

import de.polyas.core3.vep.verif.ballot.decoding.DecodedBallot
import de.polyas.core3.vep.verif.ballot.decoding.DecodedCandidate
import de.polyas.core3.vep.verif.ballot.decoding.DecodedCandidateList
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateListSpec

fun ballotChoice(ballot: BallotSpec, block: BallotChoiceCtx.() -> Unit): DecodedBallot {
    val ctx = BallotChoiceCtx(ballot)
    ctx.block()
    check(ctx.listChoices.size == ballot.lists.size) {
        "Not enough lists (only ${ctx.listChoices.size}) in ballot with id ${ballot.id} and title ${ballot.title.default}; expected number is ${ballot.lists.size}"
    }
    return DecodedBallot(
        id = ballot.id,
        intentionallyInvalid = ctx.intentionallyInvalid,
        abstained = ctx.abstain,
        tooLong = ctx.tooLong,
        decodedLists = ctx.listChoices
    )
}

class BallotChoiceCtx(private val ballot: BallotSpec) {
    var listIndex = 0
    internal val listChoices = mutableListOf<DecodedCandidateList>()
    internal var intentionallyInvalid: Boolean = false
    internal var abstain: Boolean = false
    internal var tooLong: Boolean = false

    fun list(votesForList: Int = 0, block: ListChoiceCtx.() -> Unit)   {
        val list = ballot.lists.getOrElse(listIndex) {
            error("Too many lists in ballot with id ${ballot.id} and title ${ballot.title.default}")
        }
        listIndex += 1
        val ctx = ListChoiceCtx(list)
        ctx.block()
        check(ctx.choices.size == list.candidates.size) {
            "Not enough choice entries (only ${ctx.choices.size}) in list with id ${list.id} and title ${list.title?.default}; expected number is ${list.candidates.size}"
        }
        listChoices += DecodedCandidateList(list.id, votesForList, ctx.choices)
    }
}

class ListChoiceCtx(private val list: CandidateListSpec) {
    private var choiceIndex = 0
    internal val choices = mutableListOf<DecodedCandidate>()

    operator fun Int.not() = nextChoice(this, null)
    operator fun Pair<Int, String>.not() = nextChoice(first, second)

    private fun nextChoice(votes: Int, writeInContent: String?) {
        val candidateSpec = list.candidates.getOr(choiceIndex) {
            error("Too many choice entries in list with id ${list.id} and title ${list.title?.default}")
        }
        choiceIndex += 1
        choices += DecodedCandidate(candidateSpec.id, votes, writeInContent)
    }
}

private fun<T> List<T>.getOr(index: Int, onOutOfBound: () -> T) : T =
    when {
        index !in indices -> onOutOfBound()
        else -> this[index]
    }
