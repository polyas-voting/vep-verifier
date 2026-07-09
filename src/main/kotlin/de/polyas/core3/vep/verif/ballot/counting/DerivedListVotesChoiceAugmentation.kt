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

/**
 * Used for candidate lists with defined [CandidateListSpec.derivedListVotes]
 * (where votes for candidates are also counted as votes for the list).
 */
internal class DerivedListVotesChoiceAugmentation(ballotSpec: BallotSpec) : ChoiceAugmentation {

    override fun augmentChoices(inputSequence: Sequence<SelectedChoice>): Sequence<SelectedChoice> {
        return compactify(augmentChoicesNotCompacted(inputSequence))
    }

    /**
     * Adds the extra votes for lists, but there may be multiple entries for the same list (ID)
     */
    private fun augmentChoicesNotCompacted(inputSequence: Sequence<SelectedChoice>): Sequence<SelectedChoice> {
        val originalChoices = inputSequence.toList()

        // we will add the derived votes only for lists which were not originally selected
        val originallySelected = originalChoices
            .filter { it.numberOfVotes > 0 }
            .map { it.choiceId }
            .toSet()

        return sequence {
            val alreadyIncreased: MutableSet<String> = mutableSetOf() // used for the "at most one" case

            originalChoices.forEach { selectedChoice ->
                yield (selectedChoice)

                // The derivation maps are indexed by the base candidate (relevant for write-int candidates)
                val baseChoiceId = selectedChoice.baseId()

                candidateToListEachCounts[baseChoiceId]?.let { listId ->
                    yield(SelectedChoice(listId, selectedChoice.numberOfVotes))
                }

                candidateToListAtMostOnce[baseChoiceId]?.let { listId ->
                    if (listId !in alreadyIncreased && listId !in originallySelected) {
                        yield (SelectedChoice(listId, 1))
                        alreadyIncreased.add(listId)
                    }
                }
            }
        }
    }

    private fun enabledDeriveEachCounts(list: CandidateListSpec): Boolean =
        list.derivedListVotes?.variant == CandidateListSpec.DerivedListVotesSpec.Variant.EACH_VOTE_COUNTS

    private fun enabledDeriveAtMostOne(list: CandidateListSpec): Boolean =
        list.derivedListVotes?.variant == CandidateListSpec.DerivedListVotesSpec.Variant.AT_MOST_ONE

    private val candidateToListEachCounts: Map<String, String> = ballotSpec.lists.asSequence()
        .filter(this::enabledDeriveEachCounts)
        .flatMap { list -> list.candidates.asSequence().map { candidate -> candidate.id to list.id } }
        .toMap()

    private val candidateToListAtMostOnce: Map<String, String> = ballotSpec.lists.asSequence()
        .filter(this::enabledDeriveAtMostOne)
        .flatMap { list -> list.candidates.asSequence().map { candidate -> candidate.id to list.id } }
        .toMap()
}

private fun compactify(choices: Sequence<SelectedChoice>): Sequence<SelectedChoice> =
    choices.groupingBy { it.choiceId }
        .fold(0) { acc, x -> acc + x.numberOfVotes }
        .asSequence()
        .map { (id, count) -> SelectedChoice(id, count) }
