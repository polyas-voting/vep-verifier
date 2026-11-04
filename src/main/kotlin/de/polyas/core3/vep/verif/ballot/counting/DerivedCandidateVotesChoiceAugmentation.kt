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

import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateListSpec

internal class DerivedCandidateVotesChoiceAugmentation(private val ballotSpec: BallotSpec) :
    ChoiceAugmentation {

    private val allChoiceIds = ballotSpec.lists.flatMap { list -> list.candidates.map { it.id } + list.id }
    private val allCandidateChoiceIds = ballotSpec.lists.flatMap { list -> list.candidates.map { it.id } }

    override fun augmentChoices(inputSequence: Sequence<SelectedChoice>): Sequence<SelectedChoice> {
        val choiceAccumulator = ChoiceAccumulator(ballotSpec, inputSequence)

        val relevantLists = ballotSpec.lists
            .filter { list -> list.derivedCandidateVotes != null }
            .filter { list -> (choiceAccumulator.votes(list.id) ?: 0) > 0 }

        check(relevantLists.size <= 1) { "More than one list selected for candidate votes derivation: $relevantLists" }

        val votesOnBallotTotal = allChoiceIds.sumOf { choiceAccumulator.votes(it) ?: 0 }
        val votesForCandidatesTotal = allCandidateChoiceIds.sumOf { choiceAccumulator.votes(it) ?: 0 }

        for (list in relevantLists) {
            distributeVotesInList(list, votesOnBallotTotal, votesForCandidatesTotal, choiceAccumulator)
        }

        return choiceAccumulator.votesAsSequence()
    }

    private fun distributeVotesInList(
        list: CandidateListSpec,
        votesOnBallotTotal: Int,
        votesForCandidatesTotal: Int,
        choiceAccumulator: ChoiceAccumulator
    ) {
        val spec = list.derivedCandidateVotes ?: error("Missing derived candidate spec")
        val votesOnList = list.candidates.sumOf { candidate -> choiceAccumulator.votes(candidate.id) ?: 0 }
        val votesForList = choiceAccumulator.votes(list.id) ?: 0
        val votesToDistribute = listOfNotNull(
            list.maxVotesOnList - votesOnList,
            ballotSpec.maxVotes - votesOnBallotTotal,
            ballotSpec.maxVotesForCandidates?.let { it - votesForCandidatesTotal },
            list.maxVotesTotal?.let { it - votesOnList - votesForList },
        ).minOf { it }

        when (spec.variant) {
            CandidateListSpec.DerivedCandidateVotesSpec.Variant.AT_MOST_ONE ->
                distributeVotesAtMostOne(list, spec.excludeSelectedCandidates, votesToDistribute, choiceAccumulator)

            CandidateListSpec.DerivedCandidateVotesSpec.Variant.AS_MUCH_AS_POSSIBLE ->
                distributeVotesAsMuchAsPossible(list, spec.excludeSelectedCandidates, votesToDistribute, choiceAccumulator)
        }
    }

    /**
     * Distributes votes to the candidates of the given [list]. It distributes at most one vote
     * to each candidate (as long as it does not violate the `candidate.maxVotes` limit) and
     * not more than the total of [votesToDistribute].
     */
    private fun distributeVotesAtMostOne(
        list: CandidateListSpec,
        excludeSelected: Boolean,
        votesToDistribute: Int,
        choiceAccumulator: ChoiceAccumulator,
    ) {
        if (votesToDistribute <= 0) return
        var votesLeft = votesToDistribute

        val candidatesTakingPartInDistribution =
            if (excludeSelected) list.candidates.filter { choiceAccumulator.votes(it.id) == 0 }
            else list.candidates

        for (candidate in candidatesTakingPartInDistribution) {
            val votesForCandidate = choiceAccumulator.votes(candidate.id) ?: continue
            if (votesForCandidate < candidate.maxVotes) {
                choiceAccumulator.set(candidateId = candidate.id, newValue = votesForCandidate + 1)
                votesLeft -= 1
                if (votesLeft <= 0) return
            }
        }
    }

    /**
     * Distributes votes to the candidates of the given [list]. It distributes the biggest
     * possible number of votes which does not violate `candidate.maxVotes`, but not more than
     * [votesToDistribute].
     */
    private fun distributeVotesAsMuchAsPossible(
        list: CandidateListSpec,
        excludeSelected: Boolean,
        votesToDistribute: Int,
        choiceAccumulator: ChoiceAccumulator,
    ) {
        if (votesToDistribute <= 0) return
        var votesLeft = votesToDistribute

        val candidatesTakingPartInDistribution =
            if (excludeSelected) list.candidates.filter { choiceAccumulator.votes(it.id) == 0 }
            else list.candidates

        while (true) {
            val leftBefore = votesLeft
            for (candidate in candidatesTakingPartInDistribution) {
                val votesForCandidate = choiceAccumulator.votes(candidate.id) ?: continue
                if (votesForCandidate < candidate.maxVotes) {
                    choiceAccumulator.set(candidate.id, votesForCandidate + 1)
                    votesLeft -= 1
                    if (votesLeft <= 0) return
                }
            }
            if (leftBefore == votesLeft) return // no progress in the whole round, abort
        }
    }
}

/**
 * Maintains a mutable assignment of the number of votes to actual choices and provides
 * access (via method [votes]) and update (via method [set]) functionality by candidate/list
 * identifiers (as opposed to actual choice identifiers). See [choiceIdMapper] for the
 * explanation of the difference between the two.
 */
private class ChoiceAccumulator(
    ballotSpec: BallotSpec,
    inputSequence: Sequence<SelectedChoice>
) {
    private val votesById: MutableMap<String, Int> = votesById(inputSequence)
    private val actualChoiceId: (String) -> String? = choiceIdMapper(ballotSpec, votesById)

    /**
     * Returns the (current) number of votes (as determined by the maintained mutable state)
     * for the actual choice ID corresponding to the given candidate/list identifier [candidateId].
     * The returned value may be `null`, if the [candidateId] refers to an empty write-in candidate.
     */
    fun votes(candidateId: String): Int? {
        val choiceId = actualChoiceId(candidateId) ?: return null
        return votesById[choiceId] ?: 0
    }

    /**
     * Sets the new value for the actual choice ID corresponding to the given candidate/list identifier
     * [candidateId]. It will fail if this actual choice is not defined, that is for [candidateId] that
     * method `votes` returns `null`.
     */
    fun set(candidateId: String, newValue: Int) {
        val choiceId = actualChoiceId(candidateId)
            ?: error("Unknown candidate/list: $candidateId, assignment $newValue")
        votesById[choiceId] = newValue
    }

    fun votesAsSequence(): Sequence<SelectedChoice> {
        return votesById.asSequence().map { SelectedChoice(it.key, it.value) }
    }
}

private fun votesById(inputSequence: Sequence<SelectedChoice>) =
    mutableMapOf<String,Int>().also { map ->
        inputSequence.forEach { map[it.choiceId] = it.numberOfVotes }
    }

/**
 * Computes a mapping from candidate/list identifiers, referred to  in the ballot
 * definition ([ballot]), to actual choice identifiers, as referred to in the counted
 * result ([votesById]).
 *
 * The choice identifiers differ from the corresponding candidate/list identifier
 * only for write-in fields. In this case, the returned mapping may, for instance,
 * map `idA` to `idA|yyy`, if [votesById] contain a vote for write-in
 * candidate `idA|yyy`.
 */
private fun choiceIdMapper(ballot: BallotSpec, votesById: MutableMap<String, Int>): (String) -> String? =
    if (ballot.containsWriteIns()) choiceIdByCandidateId(ballot, votesById)::get
    else { id -> id }

/**
 * Builds a map used to implement [choiceIdMapper] for the case with write-ins.
 */
private fun choiceIdByCandidateId(ballot: BallotSpec, votesById: MutableMap<String, Int>): Map<String, String> =
    buildMap {
        for (list in ballot.lists) {
            put(list.id, list.id)

            for (candidate in list.candidates) {
                if (candidate.writeInSize == null) {
                    put(candidate.id, candidate.id)
                }
            }

            val writeInCandidateIds = writeInCandidateIds(list)
            if (writeInCandidateIds.isNotEmpty()) {
                for (actualChoiceId in votesById.keys) {
                    val candidateId = actualChoiceId.takeWhile { it != WRITE_IN_SEPARATOR }
                    if (candidateId in writeInCandidateIds) {
                        put(candidateId, actualChoiceId)
                    }
                }
            }
        }
    }

private fun writeInCandidateIds(list: CandidateListSpec): Set<String> =
    list.candidates
        .filter { candidate -> candidate.writeInSize != null }
        .map { it.id }
        .toSet()

private fun BallotSpec.containsWriteIns(): Boolean =
    lists.any { it.containsWriteIns() }

private fun CandidateListSpec.containsWriteIns(): Boolean =
    candidates.any { it.writeInSize != null }
