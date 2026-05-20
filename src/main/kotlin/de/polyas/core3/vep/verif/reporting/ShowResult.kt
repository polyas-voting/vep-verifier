/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.reporting

import de.polyas.core3.vep.verif.ballot.BallotResult
import de.polyas.core3.vep.verif.ballot.Counting
import de.polyas.core3.vep.verif.types.BallotSpec
import de.polyas.core3.vep.verif.types.CandidateSpec
import de.polyas.core3.vep.verif.types.I18n.Language
import de.polyas.core3.vep.verif.types.MessageWithProofPacket
import de.polyas.core3.vep.verif.types.Registry

fun showResult(
    registry: Registry,
    decryptedPackets: () -> Sequence<MessageWithProofPacket>,
    lang: Language = Language.DE
) {
    val results = Counting(registry).count(decryptedPackets)
    formatBallotResults(registry, results, lang)
}

private fun formatBallotResults(registry: Registry, results: List<BallotResult>, lang: Language) = fmt {
    val ballotSpecById: Map<String, BallotSpec> = registry.ballotStructures.associateBy { it.id }

    +"RESULT:"

    results.forEach { ballotResult ->
        val ballotSpec = ballotSpecById[ballotResult.ballotId]
        check(ballotSpec != null) { "Ballot specification missing for ballot id ${ballotResult.ballotId}" }
        formatBallotResult(ballotResult, ballotSpec, lang)
    }
}

private fun Fmt.formatBallotResult(result: BallotResult, ballotSpec: BallotSpec, lang: Language) {

    val votes: Map<String, Int> = result.castedVotes.associateBy({ it.candidate }, { it.votes }) // choiceId -> #votes

    +""

    framed("Ballot [${result.ballotId}]: ${ballotSpec.title[lang]}") {

        +"Cast sheets: ${result.castSheets}, valid: ${result.validSheets}, malformed: ${result.malformed}, marked as abstention: ${result.abstained}, intentionally invalid: ${result.intentionallyInvalid}, invalid: ${result.invalidSheets}"

        for (list in ballotSpec.lists) {
            val headers = list.columnHeaders.joinToString(separator = " | ") { it[lang] }
            val title = list.title?.get(lang) ?: "(no title)"
            +""
            framed("List [${list.id}]: $title") {
                +"headers: $headers"
                +""
                for (candidate in list.candidates) {
                    +"[${candidate.id}]: ${candidate.asString(lang)}  -> ${votes[candidate.id] ?: 0} vote(s)"
                }
            }
        }
    }
}

private fun CandidateSpec.asString(lang: Language) : String =
    columns.joinToString(separator = ", ") { it.asPlainString(lang) }
