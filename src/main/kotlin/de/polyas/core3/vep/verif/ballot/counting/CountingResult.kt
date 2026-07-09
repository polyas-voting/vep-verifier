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

import de.polyas.core3.vep.verif.ballot.model.BallotId

/**
 * Result for a ballot.
 *
 * @param cast The total number of counted ballots
 * @param malformed The number of malformed ballots, that is ballot which could not be decoded.
 *          Such ballot only occur, if the voter front-end application misbehaves or if the voter
 *          manipulates the code of this app in the browser.
 * @param abstained The number of ballot explicitly marked as abstention
 * @param invalid The number of ballot classified as invalid, because of violation of some ballot rule
 * @param markedInvalid The number of ballots explicitly marked as invalid by the voter
 * @param valid The number of ballots classified as valid
 * @param emptyInvalid The number of empty ballots classified as [invalid] (empty ballots explicitly
 *          marked as invalid do not contribute to this count)
 * @param emptyValid The number of empty ballots classified as [valid]
 * @param votes List of votes for candidates (it includes all counted votes, also the derived ones)
 * @param ballot The ballot identifier
 */
data class CountingResult(
    val ballot: BallotId,
    val cast: Int,
    val malformed: Int,
    val abstained: Int,
    val invalid: Int,
    val markedInvalid: Int,
    val valid: Int,
    val emptyInvalid: Int,
    val emptyValid: Int,
    val votes: List<VotesPerChoice>,
) {
    /**
     * @param choiceId  The candidate/choice identifier.
     * @param countedVotes  The number of counted votes for this candidate/choice
     *          (possibly including derived votes).
     * @param castVotes The number of votes for this candidate/choice cast
     *          directly by voters (not including derived votes)
     */
    data class VotesPerChoice(
        val choiceId: String,
        val castVotes: Int,
        val countedVotes: Int,
    )
}

