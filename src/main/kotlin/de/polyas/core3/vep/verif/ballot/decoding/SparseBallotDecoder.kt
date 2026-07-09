/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot.decoding

import de.polyas.core3.vep.verif.ballot.decoding.BallotDecoder.ByteSource
import de.polyas.core3.vep.verif.ballot.model.BallotEncodingSpec
import de.polyas.core3.vep.verif.ballot.model.BallotId
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateListSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateSpec
import kotlin.math.min

/**
 * Decoder for sparse ballots, optimized for ballots with many candidates/choices but few actual votes.
 */
internal class SparseBallotDecoder(
    private val ballotSpec: BallotSpec,
): SingleBallotDecoder {
    private val encoding: BallotEncodingSpec.Sparse

    /** Array of choices (list/candidate identifiers) in the order they appear in the ballot */
    private val orderedChoices: List<ChoiceSpec> = ballotSpec.choicesInTheBallotOrder().toList()

    override val ballotId: BallotId by ballotSpec::id

    init {
        require(ballotSpec.encoding is BallotEncodingSpec.Sparse)
        { "SparseBallotDecoder can be created only for ballots with the matching encoding type" }
        encoding = ballotSpec.encoding
    }

    override fun encodedBallotSize(): Int =
        encoding.encodedBallotSize

    /**
     * Decodes a sparse ballot from the ByteSource given as the receiver.
     *
     * @return decoded ballot, or null if the ballot is malformed
     */
    override fun ByteSource.decodeBallot(): DecodingResult<DecodedBallot> {
        val ballotFlags = getNextByteAsBitSet() ?: return DecodingResult.Error.TooShort  // malformed
        val intentionallyInvalid = ballotFlags[BitfieldPos.INTENTIONALLY_INVALID]
        val abstain = ballotFlags[BitfieldPos.ABSTAIN]
        val tooLong = ballotFlags[BitfieldPos.TOO_LONG]

        val decodedChoices = decodeChoices().ifError { return it }
        val decodedChoicesMap: Map<String, Choice> = decodedChoices.associateUniqueBy { it.id } ?: return DecodingResult.Error.DuplicatedChoiceIds
        val decodedLists = decodedLists(decodedChoicesMap)

        return DecodingResult.Success(
            DecodedBallot(
                id = ballotSpec.id,
                intentionallyInvalid = intentionallyInvalid,
                abstained = abstain,
                tooLong = tooLong,
                decodedLists = decodedLists
            )
        )
    }

    private fun ByteSource.decodeChoices(): DecodingResult<List<Choice>> =
        DecodingResult.Success(
            buildList {
                var position = 1  // remember reading position, 1 for ballot flags
                while (position < encoding.encodedBallotSize) {  // end when full ballot read
                    position += encoding.bytesPerChoiceId
                    val choiceIndex = getChoiceId() ?: return DecodingResult.Error.TooShort
                    if (choiceIndex == 0) {  // end when reaching end marker
                        getNextBytes(encoding.encodedBallotSize - position) ?: return DecodingResult.Error.TooShort
                        return@buildList
                    }
                    val choiceSpec = orderedChoices.getOrNull(choiceIndex - 1) ?: return DecodingResult.Error.InvalidChoiceIndex
                    val writeIn = if (choiceSpec.writeInSize > 0) {
                        position += choiceSpec.writeInSize * 2
                        getNextString(choiceSpec.writeInSize) ?: return DecodingResult.Error.TooShort
                    } else null
                    position += 1
                    val numberOfVotes = getNextByteAsInt() ?: return DecodingResult.Error.TooShort
                    add(Choice(choiceSpec.id, numberOfVotes, writeIn))
                }
            }
        )

    private fun ByteSource.getChoiceId(): Int? {
        var choiceId = 0
        repeat(encoding.bytesPerChoiceId) {
            val nextInt = getNextByteAsInt() ?: return null  // malformed
            choiceId = 256 * choiceId + nextInt
        }
        return choiceId
    }

    private fun decodedLists(choices: Map<String, Choice>): List<DecodedCandidateList> {
        return ballotSpec.lists.map { it.decodeList(choices) }
    }
}

private fun CandidateListSpec.decodeList(choices: Map<String, Choice>): DecodedCandidateList {
    val votes: Int = choices[id]?.votes ?: 0
    val decodedCandidates = candidates.map { it.decodeCandidate(choices) }
    return DecodedCandidateList(id, votes, decodedCandidates)
}

private fun CandidateSpec.decodeCandidate(choices: Map<String, Choice>): DecodedCandidate {
    val choice = choices[id]
    val votes = choice?.votes ?: 0
    val writeInContent = choice?.writeIn
    return DecodedCandidate(id, votes, writeInContent)
}

private data class Choice(
    val id: String,
    val votes: Int,
    val writeIn: String?
)

/**
 * Creates a map with keys generated by the given [keySelector] and values from the elements of this collection.
 * If duplicate keys are encountered during iteration, the function returns `null`.
 */
private inline fun <T, K> Collection<T>.associateUniqueBy(keySelector: (T) -> K): Map<K, T>? =
    associateBy(keySelector).takeIf { map -> map.size == size }

private data class ChoiceSpec(
    val index: Int,
    val id: String,
    val listId: String?,  // for candidates
    val writeInSize: Int
)

/**
 * Returns a sequence of choices (list/candidate identifiers) in the order they appear in the ballot.
 */
private fun BallotSpec.choicesInTheBallotOrder(): Sequence<ChoiceSpec> = lists.asSequence()
    .flatMap { list ->
        val candidates = list.candidates.asSequence()
            .map { candidate -> Triple(candidate.id, list.id, candidate.writeInSize ?: 0) }

        val maxVotesForList = maxVotesForLists?.let { min(it, list.maxVotesForList) } ?: list.maxVotesForList
        if (maxVotesForList > 0) {
            sequenceOf(Triple(list.id, null, 0)).plus(candidates)
        } else {
            candidates
        }
    }.mapIndexed { index, (id, listId, writeInSize) -> ChoiceSpec(index + 1, id, listId, writeInSize) }
