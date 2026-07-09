/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot.validation

import de.polyas.core3.vep.verif.ballot.decoding.DecodedBallot
import de.polyas.core3.vep.verif.ballot.decoding.DecodedCandidate
import de.polyas.core3.vep.verif.ballot.decoding.DecodedCandidateList
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateListSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateSpec
import de.polyas.core3.vep.verif.ballot.model.isWriteIn

class BallotValidator(val ballotSpec: BallotSpec) {

    private val candidateListSpec: Map<String, CandidateListSpec> = initCandidateListSpec(ballotSpec)
    private val candidateSpec: Map<String, CandidateSpec> = initCandidateSpec(ballotSpec)

    fun validateBallot(decodedBallot: DecodedBallot): Validated<DecodedBallot> {
        require(decodedBallot.id == ballotSpec.id)
            { "The identifier ${decodedBallot.id} of the validated ballot does not match the identifier ${ballotSpec.id} of the ballot specification" }

        return when {
            decodedBallot.isMalformed() -> decodedBallot.asMalformed
            decodedBallot.intentionallyInvalid -> decodedBallot.asIntentionallyInvalid
            decodedBallot.abstained -> decodedBallot.asAbstained
            decodedBallot.tooLong -> decodedBallot.asInvalid
            decodedBallot.isInvalid() -> decodedBallot.asInvalid
            else -> decodedBallot.asValid
        }
    }

    /**
     * Checks in the given decoded ballot is malformed.
     *
     * A ballot is classified as malformed if it contains
     *
     *  - wrong identifiers,
     *  - ballot is marked as invalid, while this choice is not allowed by the ballot specification
     *  - duplicate elements (duplicate list or candidates)
     *  - write-in content consistent with the ballot spec
     *
     * A ballot produced by a voter using (correctly implemented) voter client, encoded,
     * and then decoded, should always be well-formed.
     */
    private fun DecodedBallot.isMalformed(): Boolean {
        val listIds = decodedLists.map { it.id }
        if (listIds.containsDuplicates()) { return true }

        if (intentionallyInvalid && !ballotSpec.showInvalidOption) { return true }

        if (abstained && !ballotSpec.showAbstainOption) { return true }

        return decodedLists.any { list -> listIsMalformed(list) }
    }

    /**
     * Checks if a given (well-formed) ballot is invalid, because it violates ballot rules
     */
    private fun DecodedBallot.isInvalid(): Boolean =
        validate(this) != ValidationResult.VALID

    private fun validate(ballot: DecodedBallot): ValidationResult {
        for (list in ballot.decodedLists) {
            validate(list).ifInvalid { return it }
        }

        val votes = ballot.votesInBallot()
        if (votes < ballotSpec.minVotes) {
            return ValidationResult.INVALID
        }
        if (votes > ballotSpec.maxVotes) {
            return ValidationResult.INVALID
        }

        val maxListWithChoices = ballotSpec.maxListsWithChoices
        if (maxListWithChoices != null) {
            if (ballot.listsWithChoices() > maxListWithChoices) {
                return ValidationResult.INVALID
            }
        }

        val maxVotesForLists = ballotSpec.maxVotesForLists
        val minVotesForLists = ballotSpec.minVotesForLists
        if (maxVotesForLists != null || minVotesForLists != null) {
            val votesForLists = ballot.votesForLists()
            if (maxVotesForLists != null && votesForLists > maxVotesForLists) {
                return ValidationResult.INVALID
            }
            if (minVotesForLists != null && votesForLists < minVotesForLists) {
                return ValidationResult.INVALID
            }
        }

        val maxVotesForCandidates = ballotSpec.maxVotesForCandidates
        val minVotesForCandidates = ballotSpec.minVotesForCandidates
        if (maxVotesForCandidates != null || minVotesForCandidates != null) {
            val votesForCandidates = ballot.votesForCandidates()
            if (maxVotesForCandidates != null && votesForCandidates > maxVotesForCandidates) {
                return ValidationResult.INVALID
            }
            if (minVotesForCandidates != null && votesForCandidates < minVotesForCandidates) {
                return ValidationResult.INVALID
            }
        }

        return ValidationResult.VALID
    }

    private fun validate(list: DecodedCandidateList): ValidationResult {
        val spec = candidateListSpec[list.id]
        require(spec != null) { "List has a wrong identifier ${list.id}" }

        for (candidate in list.decodedCandidates) {
            validate(candidate).ifInvalid { return it }
        }

        val votesOnList = list.decodedCandidates.sumOf { it.votes }

        if (spec.voteCandidateXorList && list.votes != 0 && votesOnList != 0) {
            return ValidationResult.INVALID
        }

        if (list.votes > spec.maxVotesForList) {
            return ValidationResult.INVALID
        }
        if (list.votes < spec.minVotesForList) {
            return ValidationResult.INVALID
        }
        if (votesOnList > spec.maxVotesOnList) {
            return ValidationResult.INVALID
        }
        if (votesOnList < spec.minVotesOnList) {
            return ValidationResult.INVALID
        }

        val minVotesTotal = spec.minVotesTotal
        val maxVotesTotal = spec.maxVotesTotal
        if (minVotesTotal != null || maxVotesTotal != null) {
            val votesTotal = list.votesTotal()
            if (maxVotesTotal != null && votesTotal > maxVotesTotal) {
                return ValidationResult.INVALID
            }
            if (minVotesTotal != null && votesTotal < minVotesTotal) {
                return ValidationResult.INVALID
            }
        }

        return ValidationResult.VALID
    }

    private fun validate(candidate: DecodedCandidate): ValidationResult {
        val spec = candidateSpec[candidate.id]
        require(spec != null) { "Decoded has wrong candidate identifier ${candidate.id}" }

        if (candidate.votes > spec.maxVotes) {
            return ValidationResult.INVALID
        }
        if (candidate.votes < spec.minVotes) {
            return ValidationResult.INVALID
        }

        return ValidationResult.VALID
    }

    private fun listIsMalformed(list: DecodedCandidateList): Boolean  {
        val spec = candidateListSpec[list.id] ?: return true

        val candidateIds = list.decodedCandidates.map { it.id }
        if (candidateIds.containsDuplicates()) { return true }

        val candidateSet = spec.candidates.asSequence().map { it.id }.toSet()
        list.decodedCandidates.forEach {
            if (it.id !in candidateSet) { return true }
        }

        return list.decodedCandidates.any { candidate -> candidateIsMalformed(candidate) }
    }

    private fun candidateIsMalformed(candidate: DecodedCandidate): Boolean {
        val spec = candidateSpec[candidate.id] ?: return true

        if (spec.isWriteIn() && candidate.votes > 0 && candidate.writeInContent == null) {
            return true
        }
        if (!spec.isWriteIn() && candidate.writeInContent != null) {
            return true
        }

        return false
    }

}

private fun initCandidateListSpec(ballotSpec: BallotSpec): Map<String, CandidateListSpec> =
    ballotSpec.lists.associateBy(CandidateListSpec::id)

private fun initCandidateSpec(ballotSpec: BallotSpec): Map<String, CandidateSpec> =
    ballotSpec.lists
        .flatMap(CandidateListSpec::candidates)
        .associateBy(CandidateSpec::id)

private fun List<*>.containsDuplicates(): Boolean =
    distinct().size != size

private enum class ValidationResult {
    VALID, INVALID;

    inline fun ifInvalid(block: (ValidationResult) -> ValidationResult): ValidationResult =
        if (this == INVALID)  block(this)
        else this
}