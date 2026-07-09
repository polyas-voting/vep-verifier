/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BallotSpec(
    val id: BallotId,

    /** The binary encoding specification of cast ballots */
    val encoding: BallotEncodingSpec = BallotEncodingSpec.Standard,

    /** Optional content to be displayed at the top of the ballot */
    val contentAbove: AnyContent?,

    /** Title of the ballot */
    val title: I18nString,

    /** Sequence of candidate lists */
    val lists: List<CandidateListSpec>,

    /** Optional content to be displayed at the bottom of the ballot */
    val contentBelow: AnyContent?,

    /** If true, a checkbox is shown for explicitly marking the ballot as invalid */
    val showInvalidOption: Boolean,

    /** If true, an abstention checkbox is shown */
    val showAbstainOption: Boolean,

    /** The maximal total number of votes (crosses) of all kinds a voter can select in the whole ballot; ballots with bigger number of crosses will be rejected as invalid */
    val maxVotes: Int,

    /** The lower bound on the total number of votes a voter can select in the whole ballot; ballots with smaller number of crosses will be rejected as invalid */
    val minVotes: Int,

    /** The maximal allowed number of votes for lists in the whole ballot */
    val maxVotesForLists: Int?,

    /** The minimal required number of votes for lists in the whole ballot */
    val minVotesForLists: Int?,

    /** The maximal allowed number of votes for candidates (as opposed to votes for lists) in the whole ballot */
    val maxVotesForCandidates: Int?,

    /** The minimal required number of votes for candidates (as opposed to votes for lists) in the whole ballot */
    val minVotesForCandidates: Int?,

    /** The maximum number of lists where the voter can place his/her choices. If `null`, there are no restrictions. */
    val maxListsWithChoices: Int?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CandidateListSpec(
    /** The identifier of the list, unique in the scope of the electionTemplate */
    val id: String,

    /** Optional content to be displayed above the title */
    val contentAbove: AnyContent?,

    /** The title (headline) of the candidate list (optional) */
    val title: I18nString?,

    /** List of column headers */
    val columnHeaders: List<I18nString>,

    /** List of candidate specifications */
    val candidates: List<CandidateSpec>,

    /** The maximal allowed total number of votes (for regular candidates) on the list */
    val maxVotesOnList: Int,

    /** The minimal required total number of votes (for regular candidates) on the list */
    val minVotesOnList: Int,

    /** The maximal allowed number of votes for this list (crosses next to the list header). Non-zero value is used for elections with list voting. */
    val maxVotesForList: Int,

    /** The minimal required number of votes for this list (crosses next to the list header) */
    val minVotesForList: Int,

    /** The maximal allowed total number of votes (of both types: on and for the list) */
    val maxVotesTotal: Int?,

    /** The minimal required total number of votes (of both types: on and for the list) */
    val minVotesTotal: Int?,

    /** If true, the voter is not allowed to place votes on the list and for the list at the same time */
    val voteCandidateXorList: Boolean,

    /** Optional specification of derived list votes, where votes for candidates count as votes for the list */
    val derivedListVotes: DerivedListVotesSpec?,

    /** Optional specification of derived candidate votes, where votes for candidates are derived from votes for the list */
    val derivedCandidateVotes: DerivedCandidateVotesSpec?,
) {

    data class DerivedListVotesSpec(val variant: Variant) {
        enum class Variant {
            /** Each vote for a candidate is counted as a vote for the list. */
            EACH_VOTE_COUNTS,

            /** The list gets exactly one extra vote, if at least one candidate on this list is selected. */
            AT_MOST_ONE
        }
    }

    data class DerivedCandidateVotesSpec(
        val variant: Variant = Variant.AT_MOST_ONE,

        /** If true, the candidates with explicit votes given by the voter are will not obtain additional derived votes */
        val excludeSelectedCandidates: Boolean = false
    ) {
        /** The variant of the vote derivation method, specifying how many derived votes a single candidate can obtain */
        enum class Variant {
            /** Each candidate can be assigned at most one derived vote */
            AT_MOST_ONE,

            /** A candidate can be assigned more than one derived vote, if it does not contradict other rules */
            AS_MUCH_AS_POSSIBLE
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CandidateSpec(
    /** The identifier of the candidate, unique in the scope of electionTemplate */
    val id: String,

    /** The content of the consecutive columns describing the candidate; the number of columns should correspond to the number of the column headers in the enclosing `CandidateList` */
    val columns: List<AnyContent>,

    /** The maximal number of votes that can be given to the candidate */
    val maxVotes: Int,

    /** The minimal required number of votes that must be given to the candidate */
    val minVotes: Int,

    /** If non-null and non-zero, the field represents a write-in with the given size, where the content is used as a placeholder */
    val writeInSize: Int? = null,
)

fun CandidateSpec.isWriteIn(): Boolean =
    writeInSize != null && writeInSize > 1

fun CandidateSpec.effectiveWriteInSize(): Int =
    writeInSize ?: 0

@JsonTypeInfo(
    use = JsonTypeInfo.Id.SIMPLE_NAME,
    include = JsonTypeInfo.As.PROPERTY,  // serialize Custom type only as JsonBacked
    property = "type",
)
@JsonIgnoreProperties(ignoreUnknown = true)  // ignore type in Standard/Sparse
/** Type of the encoding used for the ballot, possibly with additional parameters */
sealed interface BallotEncodingSpec {

    data object Standard : BallotEncodingSpec

    data class Sparse(
        /** Number of bytes needed to encode one (each) choice index */
        val bytesPerChoiceId: Int,
        /** The expected size of the encoded ballot in bytes */
        val encodedBallotSize: Int
    ) : BallotEncodingSpec
}

/** Ballot identifier (represented as a string) */
@JvmInline
value class BallotId(val value: String) {
    override fun toString(): String = value
}
