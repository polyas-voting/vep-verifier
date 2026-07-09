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

import com.fasterxml.jackson.annotation.JsonInclude
import de.polyas.core3.vep.verif.ballot.model.BallotId

/**
 * A direct representation of voter's choices.
 */
data class DecodedBallot(
    val id: BallotId,
    val intentionallyInvalid: Boolean,
    val abstained: Boolean,
    val tooLong: Boolean,
    val decodedLists: List<DecodedCandidateList>
) {
    fun votesInBallot(): Int = votesForLists() + votesForCandidates()

    fun votesForLists(): Int = decodedLists.sumOf(DecodedCandidateList::votes)

    fun isEmpty(): Boolean = votesInBallot() == 0

    fun votesForCandidates(): Int =
        decodedLists
            .flatMap(DecodedCandidateList::decodedCandidates)
            .sumOf(DecodedCandidate::votes)

    fun listsWithChoices(): Int {
        val listsWithChoices = decodedLists.filter { list ->
            list.votes > 0 || list.decodedCandidates.any { candidate -> candidate.votes > 0 }
        }
        return listsWithChoices.count()
    }
}

data class DecodedCandidateList(
    val id: String,
    val votes: Int,
    val decodedCandidates: List<DecodedCandidate>
) {
    fun votesTotal(): Int = votes + decodedCandidates.sumOf(DecodedCandidate::votes)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DecodedCandidate(
    val id: String,
    val votes: Int,
    val writeInContent: String?
)
