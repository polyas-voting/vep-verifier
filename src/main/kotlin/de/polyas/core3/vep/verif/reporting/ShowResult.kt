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

import de.polyas.core3.vep.verif.ballot.counting.Counting
import de.polyas.core3.vep.verif.ballot.counting.CountingResult
import de.polyas.core3.vep.verif.ballot.model.*
import de.polyas.core3.vep.verif.types.MessageWithProofPacket
import de.polyas.core3.vep.verif.types.Registry

fun showResult(
    registry: Registry,
    decryptedPackets: () -> Sequence<MessageWithProofPacket>,
    lang: Language = Language.DE
) {
    val openVotes = decryptedPacketsToOpenVotes(decryptedPackets())
    val results = Counting(registry.ballotStructures).count(openVotes)
    formatBallotResults(registry, results, lang)
}

private fun decryptedPacketsToOpenVotes(decryptedPackets: Sequence<MessageWithProofPacket>): Sequence<OpenedVote> =
    decryptedPackets.flatMap { packet ->
        packet.messagesWithZKP.asSequence().map { OpenedVote(it.message.asHexString(), packet.publicLabel) }
    }

private fun formatBallotResults(registry: Registry, results: List<CountingResult>, lang: Language) = fmt {
    val ballotSpecById: Map<BallotId, BallotSpec> = registry.ballotStructures.associateBy { it.id }

    +"RESULT:"

    results.forEach { ballotResult ->
        val ballotSpec = ballotSpecById[ballotResult.ballot]
        check(ballotSpec != null) { "Ballot specification missing for ballot id ${ballotResult.ballot}" }
        formatBallotResult(ballotResult, ballotSpec, lang)
    }
}

private fun Fmt.formatBallotResult(result: CountingResult, ballotSpec: BallotSpec, lang: Language) {

    val votes: Map<String, Int> = result.votes.associateBy({ it.choiceId }, { it.countedVotes }) // choiceId -> #votes

    +""

    framed("Ballot [${result.ballot}]: ${ballotSpec.title[lang]}") {
        +"Cast sheets: ${result.cast}, valid: ${result.valid}, malformed: ${result.malformed}, marked as abstention: ${result.abstained}, intentionally invalid: ${result.markedInvalid}, invalid: ${result.invalid}"

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
