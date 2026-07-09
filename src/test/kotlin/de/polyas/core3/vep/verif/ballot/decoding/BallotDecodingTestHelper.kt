package de.polyas.core3.vep.verif.ballot.decoding

import de.polyas.core3.vep.verif.ballot.model.*

fun ballot(
    maxVotes: Int,
    maxVotesForLists: Int?,
    maxVotesForCandidates: Int?,
    builder: BallotBuilder.() -> Unit
): BallotSpec =
    BallotSpec(
        id = BallotId("ballot-id"),
        encoding = BallotEncodingSpec.Standard,
        contentAbove = null,
        contentBelow = null,
        maxVotes = maxVotes,
        minVotes = 0,
        maxListsWithChoices = null,
        title = I18n.of("ballot title"),
        lists = BallotBuilder().run { builder(); build() },
        minVotesForLists = null,
        maxVotesForLists = maxVotesForLists,
        minVotesForCandidates = null,
        maxVotesForCandidates = maxVotesForCandidates,
        showInvalidOption = false,
        showAbstainOption = false,
    )

fun BallotSpec.withSparseEncoding(encodedBallotSize: Int, byttesPerChoice: Int = 1): BallotSpec =
    copy(encoding = BallotEncodingSpec.Sparse(bytesPerChoiceId = byttesPerChoice, encodedBallotSize = encodedBallotSize))

class BallotBuilder {
    private val lists = mutableListOf<CandidateListSpec>()

    fun list(
        id: String = IdDispenser.nextId(),
        maxVotesTotal: Int?,
        maxVotesForList: Int,
        maxVotesOnList: Int,
        builder: CandidateListBuilder.() -> Unit
    ) {
        lists += CandidateListSpec(
            id = id,
            title = I18n.of("List"),
            contentAbove = null,
            minVotesForList = 0,
            minVotesOnList = 0,
            minVotesTotal = null,
            maxVotesForList = maxVotesForList,
            maxVotesOnList = maxVotesOnList,
            maxVotesTotal = maxVotesTotal,
            columnHeaders = listOf(I18n.of("List")),
            candidates = CandidateListBuilder().run { builder(); build() },
            voteCandidateXorList = false,
            derivedListVotes = null,
            derivedCandidateVotes = null,
        )
    }

    fun build(): List<CandidateListSpec> = lists
}

class CandidateListBuilder {
    private val candidates = mutableListOf<CandidateSpec>()

    fun candidate(
        id: String = IdDispenser.nextId(),
        maxVotes: Int,
        writeInSize: Int = 0
    ) {
        candidates += CandidateSpec(
            id = id,
            minVotes = 0,
            maxVotes = maxVotes,
            writeInSize = writeInSize.takeIf { it > 0 },
            columns = listOf(),
        )
    }

    fun build(): List<CandidateSpec> = candidates
}

internal fun SingleBallotDecoder.decodeBallot(ballotBytesHex: String): DecodingResult<DecodedBallot> {
    val ballotBytes =  ballotBytesHex.hexToByteArray()
    val byteSources = BallotDecoder.ByteSource(ballotBytes)
    return byteSources.decodeBallot()
}

private object IdDispenser {
    private var id: Int = 1

    fun nextId(): String = "##-${id++}"
}

internal fun <T> DecodingResult<T>.assertSuccess(): T =
    this.ifError {
        error("Decoding failed with: $this")
    }