/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot.counting

import de.polyas.core3.vep.verif.ballot.decoding.BallotDecoder
import de.polyas.core3.vep.verif.ballot.decoding.DecodedBallot
import de.polyas.core3.vep.verif.ballot.model.BallotId
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.isWriteIn
import de.polyas.core3.vep.verif.ballot.model.OpenedVote
import de.polyas.core3.vep.verif.ballot.validation.BallotValidator
import de.polyas.core3.vep.verif.ballot.validation.Validated
import de.polyas.core3.vep.verif.ballot.validation.asMalformed
import de.polyas.core3.vep.verif.ballot.validation.asValid
import de.polyas.core3.vep.verif.ballot.validation.furtherValidate
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Counting of the election result given as a sequence of open ballots or decoded ballots.
 *
 * Parametrised by the ballot specification.
 */
class Counting(private val ballotSpecs: List<BallotSpec>) {

    private val ballotDecoder: BallotDecoder =  BallotDecoder(ballotSpecs)
    private val validators: Map<BallotId, BallotValidator> = validatorsByBallotId(ballotSpecs)

    /**
     * Counts the election result given as a stream of open ballots.
     *
     * Each open ballot may result in a sequence of decoded ballots to be counted.
     *
     * @return List of object of type [de.polyas.core3.vep.verif.ballot.counting.CountingResult], each describing the result for one ballot.
     */
    fun count(openVotes: Sequence<OpenedVote>): List<CountingResult> =
        countDecodedValidatedBallots(openVotes.flatMap(this::decodeOpenBallot))

    internal fun countDecodedBallots(decodedBallots: Sequence<DecodedBallot>): List<CountingResult> =
        countDecodedValidatedBallots(decodedBallots.map { it.asValid })

    /**
     * Counts the election result given as a stream of validated decoded ballots with auxiliary data.
     *
     * @return List of object of type [CountingResult], each describing the result for one candidate list.
     */
    private fun countDecodedValidatedBallots(decodedBallots: Sequence<Validated<DecodedBallot>>): List<CountingResult> {
        val resultAccumulators: Map<BallotId, ResultAccumulator> =
            ballotSpecs.associate { it.id to ResultAccumulator(it) }

        decodedBallots
            .map { validated -> furtherValidateBallot(validated) }
            .forEach { validated ->
                val accumulator = resultAccumulators[validated.value.id]
                require(accumulator != null) { "Accumulator is null for a validated ballot with id ${validated.value.id}" }
                accumulator.accumulate(validated)
            }

        return resultAccumulators.values.map(ResultAccumulator::result)
    }

    /**
     * Applies the appropriate validator (the validator corresponding to the type of decoded ballot)
     * to the given (already partially validated) decoded ballot
     */
    private fun furtherValidateBallot(partiallyValidated: Validated<DecodedBallot>): Validated<DecodedBallot> =
        partiallyValidated.furtherValidate { decodedBallot ->
            validators[decodedBallot.id]
                ?.validateBallot(decodedBallot)
                ?: decodedBallot.asMalformed
        }

    /**
     * Returns the stream of validated ballots together with the auxiliary data, obtained from the
     * given [openBallot].
     */
    private fun decodeOpenBallot(openBallot: OpenedVote): Sequence<Validated<DecodedBallot>> =
        ballotDecoder.decodeBallot(openBallot).asSequence()
}

data class SelectedChoice(val choiceId: String, val numberOfVotes: Int) {
    /**
     * The base choice identifier. It is different from [choiceId] only for write-in choices,
     * where it contains only the base part odf the [choiceId] (before the [WRITE_IN_SEPARATOR]).
     */
    fun baseId(): String =
        choiceId.takeWhile { it != WRITE_IN_SEPARATOR }
}

/**
 * A mutable result accumulator which accumulates, one-by-one, validated decoded ballots.
 */
private class ResultAccumulator(val ballotSpec: BallotSpec) {

    inner class ResultBundle(
        var castedSheets: Int = 0,
        var invalidSheets: Int = 0,
        var abstainedSheets: Int = 0,
        var malformedSheets: Int = 0,
        var markedInvalid: Int = 0,
        var validSheets: Int = 0,
        var emptyInvalidSheets: Int = 0,
        var emptyValidSheets: Int = 0,
        val countedVotes: MutableMap<String, Int> = initCastedVotes(ballotSpec), // candidateId -> count
        val castVotes: MutableMap<String, Int> = initCastedVotes(ballotSpec),    // candidateId -> count
    )

    /**
     * For some ballot configuration, extra "deduced" choices need to be added, such as
     * automatic choices for the list, when a candidate on this list is selected. This
     * object is used to add such choices.
     */
    val choiceAugmentation: ChoiceAugmentation = choiceAugmentation(ballotSpec)
    val result = ResultBundle()

    fun accumulate(validatedBallot: Validated<DecodedBallot>) {
        val isEmptyInvalid = validatedBallot.isEmptyInvalid()

        // update the main result
        result.accumulate(validatedBallot, isEmptyInvalid)
    }

    fun ResultBundle.accumulate(validatedBallot: Validated<DecodedBallot>, isEmptyInvalid: Boolean) {
        castedSheets += 1
        when (validatedBallot) {
            is Validated.Malformed -> ++malformedSheets
            is Validated.Abstained -> ++abstainedSheets
            is Validated.Invalid -> ++invalidSheets
            is Validated.IntentionallyInvalid -> ++markedInvalid
            is Validated.Valid -> {
                ++validSheets
                if (validatedBallot.value.isEmpty()) {
                    ++emptyValidSheets
                }

                // accumulate directly cast votes (not including the derived/augmented ones)
                validatedBallot.value.selectedChoices()
                    .forEach { (id, votes) ->
                        castVotes.merge(id, votes, Int::plus)
                    }

                // accumulate all counted votes (including the derived/augmented ones)
                validatedBallot.value.augmentedChoices()
                    .forEach { (id, votes) ->
                        countedVotes.merge(id, votes, Int::plus)
                    }
            }
        }
        if (isEmptyInvalid) {
            ++emptyInvalidSheets
        }
    }

    fun result(): CountingResult = CountingResult(
        ballot = ballotSpec.id,
        cast = result.castedSheets,
        malformed = result.malformedSheets,
        abstained = result.abstainedSheets,
        invalid = result.invalidSheets,
        markedInvalid = result.markedInvalid,
        valid = result.validSheets,
        emptyInvalid = result.emptyInvalidSheets,
        emptyValid = result.emptyValidSheets,
        votes = result.countedVotes.map { (id, count) ->
            CountingResult.VotesPerChoice(choiceId = id, castVotes = result.castVotes[id] ?: 0, countedVotes = count)
        }
    )

    /**
     * Returns the sequence of choices given by this decoded ballot, possibly including
     * the additional choices supplied by the [choiceAugmentation].
     */
    private fun DecodedBallot.augmentedChoices(): Sequence<SelectedChoice> =
        choiceAugmentation.augmentChoices(selectedChoices())
}

/**
 * Create a map with all the elements of the given [ballotSpec] mapped to 0.
 */
private fun initCastedVotes(ballotSpec: BallotSpec): MutableMap<String, Int> {
    val map: MutableMap<String, Int> = HashMap()
    ballotSpec.forEachVotingOption { id -> map[id] = 0 }
    return map
}

private fun BallotSpec.forEachVotingOption(block: (String) -> Unit) {
    for (list in this.lists) {
        if (list.maxVotesForList > 0) {
            block(list.id)
        }
        for (candidate in list.candidates) {
            if (!candidate.isWriteIn()) {
                block(candidate.id)
            }
        }
    }
}

private fun Validated<DecodedBallot>.isEmptyInvalid(): Boolean = (this is Validated.Invalid && this.value.isEmpty())

/**
 * Adds "derived" choices, that is choices not selected by the voter, but derived by the extra ballot rules.
 */
private fun choiceAugmentation(ballotSpec: BallotSpec): ChoiceAugmentation {
    return when {
        ballotSpec.lists.all { it.derivedListVotes != null }
            -> DerivedListVotesChoiceAugmentation(ballotSpec)

        ballotSpec.lists.all { it.derivedCandidateVotes != null }
            -> DerivedCandidateVotesChoiceAugmentation(ballotSpec)

        ballotSpec.lists.all { it.derivedListVotes == null && it.derivedCandidateVotes == null }
            -> NoAugmentation

        else -> error("Inconsistent rules for derived votes")
    }
}

/**
 * Adds derived choices, that is choices not made directly by the voter, but added based on
 * some special ballot rules.
 */
internal fun interface ChoiceAugmentation {
    fun augmentChoices(inputSequence: Sequence<SelectedChoice>): Sequence<SelectedChoice>
}

/**
 * Does not add any choices. This is the default behavior.
 */
private val NoAugmentation = ChoiceAugmentation { sequence -> sequence }

private fun validatorsByBallotId(ballotSpecs: List<BallotSpec>): Map<BallotId, BallotValidator> =
    ballotSpecs
        .map { BallotValidator(it) }
        .associateBy { it.ballotSpec.id }

/**
 * Returns the sequence of selected voting options (candidates, lists, etc.), that is the
 * voting options with non-zero number of votes, with the number of received votes.
 */
private fun DecodedBallot.selectedChoices(): Sequence<SelectedChoice> = sequence {
    for (list in decodedLists) {
        if (list.votes != 0) {
            yield (SelectedChoice(list.id, list.votes))
        }
        for (candidate in list.decodedCandidates) {
            if (candidate.votes == 0 && candidate.writeInContent.isNullOrBlank()) continue
            val id = when (val content = candidate.writeInContent) {
                null -> candidate.id
                else -> "${candidate.id}|${content.trim()}"
            }
            yield (SelectedChoice(id, candidate.votes))
        }
    }
}

internal const val WRITE_IN_SEPARATOR = '|'
