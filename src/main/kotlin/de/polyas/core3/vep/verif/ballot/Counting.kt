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

import de.polyas.core3.vep.verif.types.MessageWithProofPacket
import de.polyas.core3.vep.verif.types.Registry

class Counting(registry: Registry) {

    private val decoder = BallotDecoder(registry.ballotStructures)

    private val validatorsByBallotId: Map<String, BallotValidator> = registry.ballotStructures
        .map { ballotSpec -> BallotValidator(ballotSpec) }
        .associateBy { it.ballotSpec.id }

    /**
     * Compute the final tally (final election result), given the content of the decryption board.
     */
    fun count(decryptedPackets: () -> Sequence<MessageWithProofPacket>): List<BallotResult> {
        val openBallots = decryptedPackets().flatMap { packet ->
            packet.messagesWithZKP.asSequence().map { OpenVote(it.message.asHexString(), packet.publicLabel) }
        }
        val decodedBallots = openBallots.flatMap { decoder.decodeBallot(it).asSequence() }
        return countDecodedBallots(decodedBallots)
    }

    private fun countDecodedBallots(decodedBallots: Sequence<DecodedBallot>) : List<BallotResult> {
        val resultAccumulators = HashMap<String, ResultAccumulator>()
        decodedBallots
            .map { ballot -> validatorsByBallotId[ballot.id]?.validateBallot(ballot) ?: ballot.asMalformed }
            .forEach{ validated ->
                val accumulator = resultAccumulators.computeIfAbsent(validated.value.id, ::ResultAccumulator)
                accumulator.accumulate(validated)
            }
        return resultAccumulators.values.map(ResultAccumulator::result)
    }
}

/**
 * Mutable result accumulator. Accepts and accumulates, one by one, consecutive validated
 * decoded ballots. Method [result] returns the accumulated result.
 */
private class ResultAccumulator(val ballotId: String) {
    var castSheets: Int = 0
    var malformedSheets: Int = 0
    var invalidSheets: Int = 0
    var intentionallyInvalid: Int = 0
    var abstainedSheets: Int = 0
    var validSheets: Int = 0
    val castedVotes: MutableMap<String, Int> = mutableMapOf() // candidateId -> count

    fun accumulate(validatedBallot: Validated<DecodedBallot>) {
        ++castSheets
        when (validatedBallot) {
            is Validated.Invalid -> ++invalidSheets
            is Validated.IntentionallyInvalid -> ++intentionallyInvalid
            is Validated.Malformed -> ++malformedSheets
            is Validated.Abstained -> ++abstainedSheets
            is Validated.Valid -> {
                ++validSheets
                validatedBallot.value.forEachChosenOption { id, votes ->
                    castedVotes.merge(id, votes, Int::plus)
                }
            }
        }
    }

    fun result() : BallotResult {
        return BallotResult(
            ballotId = ballotId,
            castSheets = castSheets,
            malformed = malformedSheets,
            invalidSheets = invalidSheets,
            intentionallyInvalid = intentionallyInvalid,
            abstained = abstainedSheets,
            validSheets = validSheets,
            castedVotes = castedVotes.map { (id,count) -> BallotResult.VotePerCandidate(id, count) }
        )
    }
}

/**
 * Counting result for a single ballot sheet.
 */
data class BallotResult(
    val ballotId: String,
    val castSheets: Int,
    val malformed: Int,
    val invalidSheets: Int,
    val intentionallyInvalid: Int,
    val abstained: Int,
    val validSheets: Int,
    val castedVotes: List<VotePerCandidate>
) {
    data class VotePerCandidate(val candidate: String, val votes: Int)
}

private inline fun DecodedBallot.forEachChosenOption(block: (String, Int) -> Unit) {
    for (list in decodedLists) {
        if (list.votes != 0) {
            block(list.id, list.votes)
        }
        for (candidate in list.decodedCandidates) {
            block(candidate.id, candidate.votes)
        }
    }
}
