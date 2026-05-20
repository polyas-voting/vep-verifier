/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot

import de.polyas.core3.vep.verif.types.*

/**
 * Result of validation of a value of type [T].
 */
sealed class Validated<T> {
    abstract val value: T

    data class Malformed<T>(override val value: T) : Validated<T>()
    data class Valid<T>(override val value: T) : Validated<T>()
    data class Invalid<T>(override val value: T) : Validated<T>()
    data class IntentionallyInvalid<T>(override val value: T) : Validated<T>()
    data class Abstained<T>(override val value: T) : Validated<T>()
}

val<T> T.asMalformed get() = Validated.Valid(this)
val<T> T.asValid get() = Validated.Valid(this)
val<T> T.asInvalid get() = Validated.Invalid(this)
val<T> T.asIntentionallyInvalid get() = Validated.IntentionallyInvalid(this)
val<T> T.asAbstained get() = Validated.Abstained(this)

class BallotValidator(val ballotSpec: BallotSpec) {

    private val listSpecById: Map<String, CandidateList> = ballotSpec.lists.associateBy(CandidateList::id)

    private val candidateSpecById: Map<String, CandidateSpec> =
        ballotSpec.lists.flatMap(CandidateList::candidates).associateBy(CandidateSpec::id)

    /**
     * Validates a decoded ballot.
     *
     * @return Validated ballot (the input ballot encapsulated in Validated object)
     */
    fun validateBallot(decodedBallot: DecodedBallot): Validated<DecodedBallot> =
        with(decodedBallot) {
            check(id == ballotSpec.id) {"ballot id of the validator ($id) does not match the id of the given decoded ballot ${ballotSpec.id}"}

            if (intentionallyInvalid) { return this.asIntentionallyInvalid }
            if (abstained) { return this.asAbstained }

            val votes = votesInBallot()
            if (votes < ballotSpec.minVotes) { return this.asInvalid }
            if (votes > ballotSpec.maxVotes) { return this.asInvalid }
            if (!allListsValid()) { return this.asInvalid }

            return this.asValid
        }

    private fun DecodedBallot.allListsValid(): Boolean =
        decodedLists.all { it.isValid() }

    private fun DecodedCandidateList.isValid(): Boolean {
        val spec = listSpecById[id] ?: return false
        val votesOnList = decodedCandidates.sumOf { it.votes }
        return when {
            votes > spec.maxVotesForList -> false
            votes < spec.minVotesForList -> false
            votesOnList > spec.maxVotesOnList-> false
            votesOnList < spec.minVotesOnList -> false
            !allCandidatesValid() -> false
            else -> true
        }
    }

    private fun DecodedCandidateList.allCandidatesValid() : Boolean =
        decodedCandidates.all { it.isValid() }

    private fun DecodedCandidate.isValid() : Boolean {
        val spec = candidateSpecById[id] ?: return false
        return when {
            votes > spec.maxVotes -> false
            votes < spec.minVotes -> false
            else -> true
        }
    }

    private fun DecodedBallot.votesInBallot() : Int =
        decodedLists.map(DecodedCandidateList::votes).sum() +           // votes for lists
                decodedLists
                    .flatMap(DecodedCandidateList::decodedCandidates)
                    .map(DecodedCandidate::votes)
                    .sum()                                              // votes for candidates
}
