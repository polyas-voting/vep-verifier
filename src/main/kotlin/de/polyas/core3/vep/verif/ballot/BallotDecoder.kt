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

import de.polyas.core3.vep.verif.types.BallotSpec
import de.polyas.core3.vep.verif.types.CandidateList
import de.polyas.core3.vep.verif.types.CandidateSpec
import org.bouncycastle.util.encoders.Hex
import java.util.*

/**
 * Ballot decoding from the encoded format ([OpenVote]) to a list of [DecodedBallot].
 * This class also offers a function for computing the expected binary plaintext size.
 *
 * [OpenVote] represents a ballot which has been already decrypted (hence "open"), but still requires
 * some decoding to a more explicit format [DecodedBallot].
 *
 * The algorithms work with respect to the give list of ballot specifications `ballotSpecs`.
 */
class BallotDecoder(ballotSpecs: List<BallotSpec>) {

    private val decoders: Map<String,SingleBallotDecoder> =
        ballotSpecs
            .map { SingleBallotDecoder(it) }
            .associateBy { it.ballotSpec.id }

    /**
     * Decodes a given encoded [openBallot] and returns a list of decoded
     * ballots or null if decoding fails.
     */
    fun decodeBallot(openBallot: OpenVote): List<DecodedBallot> {
        val ballotIds = decodePublicLabel(openBallot.publicLabel)
        val ballotBytes = Hex.decode(openBallot.ballot)
        val byteSource = ByteSource(ballotBytes)
        return ballotIds.map { ballotId ->
            val decoder = decoders[ballotId] ?: error("wrong ballot id $ballotId in the public label")
            with (decoder) { byteSource.decodeBallot() }
        }
    }

    /**
     * Returns the expected ballot size or null on failure.
     */
    fun encodedPlaintextSize(publicLabel: String) : Int? {
        val decoded: List<String> = decodePublicLabel(publicLabel)
        if (decoded.any { decoders[it] == null }) return null
        return decoded.sumOf { decoders[it]?.computeLength() ?: 0 }
    }
}

private const val BITFIELDPOS_intentionallyInvalid = 0
private const val BITFIELDPOS_abstain = 1

private class SingleBallotDecoder(val ballotSpec: BallotSpec) {

    fun ByteSource.decodeBallot() : DecodedBallot {
        val bitSet = getNextByteAsBitSet()
        val intentionallyInvalid = bitSet[BITFIELDPOS_intentionallyInvalid]
        val abstained = bitSet[BITFIELDPOS_abstain]
        val decodedLists = ballotSpec.lists.map { list -> decodeList(list) }
        return DecodedBallot(
            id = ballotSpec.id,
            intentionallyInvalid = intentionallyInvalid,
            abstained = abstained,
            decodedLists = decodedLists
        )
    }

    private fun ByteSource.decodeList(spec: CandidateList) : DecodedCandidateList {
        val votes = getNextInt()
        val decodedCandidates = spec.candidates.map { decodeCandidate(it) }
        return DecodedCandidateList(spec.id, votes, decodedCandidates)
    }

    private fun ByteSource.decodeCandidate(spec: CandidateSpec) : DecodedCandidate =
        DecodedCandidate(spec.id, getNextInt())

    fun computeLength() : Int =
        1 + ballotSpec.lists.sumOf { 1 + it.candidates.size }
}

private class ByteSource(private val bytes: ByteArray) {
    private var index = 0
    private fun getNextByte() : Byte = bytes[index++]

    fun getNextByteAsBitSet(): BitSet =
        BitSet.valueOf(byteArrayOf(getNextByte()))

    fun getNextInt() : Int {
        val n = getNextByte().toInt()
        check(n >= 0) {"Expected a positive number; given $n"}
        return n
    }
}

/**
 * Open (decrypted) voter's choice along with the public label.
 */
data class OpenVote(
    val ballot: String,
    val publicLabel: String
)

data class DecodedBallot(
    val id: String,
    val intentionallyInvalid: Boolean,
    val abstained: Boolean,
    val decodedLists: List<DecodedCandidateList>
)

data class DecodedCandidateList(
    val id: String,
    val votes: Int,
    val decodedCandidates: List<DecodedCandidate>
)

data class DecodedCandidate(
    val id: String,
    val votes: Int
)

private fun decodePublicLabel(publicLabel: String) : List<String> =
    publicLabel.split(":") // public label consist of ballot identifiers separated with ":"

