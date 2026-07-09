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

import de.polyas.core3.vep.verif.ballot.decoding.BallotDecoder.ByteSource
import de.polyas.core3.vep.verif.ballot.model.BallotId
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateListSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateSpec
import de.polyas.core3.vep.verif.ballot.model.effectiveWriteInSize
import de.polyas.core3.vep.verif.ballot.model.isWriteIn

internal class StandardBallotDecoder(private val ballotSpec: BallotSpec): SingleBallotDecoder {

    override val ballotId: BallotId by ballotSpec::id

    override fun encodedBallotSize(): Int = (
        1 +
        ballotSpec.lists.sumOf { listSpec ->
            1 + listSpec.candidates.sumOf { candidateSpec -> 1 + (candidateSpec.writeInSize ?: 0)*2 }
        }
    )

    override fun ByteSource.decodeBallot(): DecodingResult<DecodedBallot> {
        val bitSet = getNextByteAsBitSet() ?: return DecodingResult.Error.TooShort
        val intentionallyInvalid = bitSet[BitfieldPos.INTENTIONALLY_INVALID]
        val abstain = bitSet[BitfieldPos.ABSTAIN]
        val tooLong = bitSet[BitfieldPos.TOO_LONG]

        val decodedLists = ballotSpec.lists.map { list -> decodeList(list).ifError { return it } }
        return DecodingResult.Success(
            DecodedBallot(
                id = ballotSpec.id,
                intentionallyInvalid = intentionallyInvalid,
                abstained = abstain,
                tooLong = tooLong,
                decodedLists = decodedLists
            )
        )
    }

    private fun ByteSource.decodeList(spec: CandidateListSpec): DecodingResult<DecodedCandidateList> {
        val votes = getNextByteAsInt() ?: return DecodingResult.Error.TooShort
        val decodedCandidates = spec.candidates.map { decodeCandidate(it).ifError { return it } }
        return DecodedCandidateList(spec.id, votes, decodedCandidates).asDecodingResult()
    }

    private fun ByteSource.decodeCandidate(spec: CandidateSpec): DecodingResult<DecodedCandidate> {
        val votes = getNextByteAsInt() ?: return DecodingResult.Error.TooShort
        val writeInContent: String? = when (spec.isWriteIn()) {
            true -> getNextString(spec.effectiveWriteInSize())
            false -> null
        }
        return DecodedCandidate(spec.id, votes, writeInContent).asDecodingResult()
    }
}
