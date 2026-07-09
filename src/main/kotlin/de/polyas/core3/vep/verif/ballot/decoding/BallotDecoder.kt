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

import de.polyas.core3.vep.verif.ballot.validation.Validated
import de.polyas.core3.vep.verif.ballot.validation.asMalformed
import de.polyas.core3.vep.verif.ballot.validation.asValid
import de.polyas.core3.vep.verif.ballot.decoding.BallotDecoder.ByteSource
import de.polyas.core3.vep.verif.ballot.model.BallotEncodingSpec
import de.polyas.core3.vep.verif.ballot.model.BallotId
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.OpenedVote
import java.util.BitSet

class BallotDecoder(ballotSpecs: List<BallotSpec>) {

    private val decoders: Map<BallotId, SingleBallotDecoder> =
        ballotSpecs.map(::singleBallotDecoder).associateBy { it.ballotId }

    fun encodedBallotSize(publicLabel: String): Int? {
        val ballotIds: List<BallotId> = decodePublicLabel(publicLabel)
        return ballotIds.sumOf { ballotId ->
            val decoder = decoders[ballotId] ?: return null
            decoder.encodedBallotSize()
        }
    }

    fun decodeBallot(openBallot: OpenedVote): List<Validated<DecodedBallot>> {
        val ballotIds: List<BallotId> = decodePublicLabel(openBallot.publicLabel)
        try {
            val ballotBytes = openBallot.ballot.hexToByteArray()
            val bytes = ByteSource(ballotBytes)
            val decodedBallots = ballotIds.map { ballotId ->
                val decoder = decoders[ballotId] ?: return allMalformed(ballotIds)
                with(decoder) {
                    bytes.decodeBallot().ifError { return allMalformed(ballotIds) } // if one is malformed, we don't continue
                }
            }
            if (!bytes.empty()) { // The open ballot is longer than expected (not consumed bytes)
                return allMalformed(ballotIds)
            }

            return decodedBallots.map { it.asValid }
        } catch (_: Exception) {
            return allMalformed(ballotIds)
        }
    }

    private fun allMalformed(ballotIds: List<BallotId>): List<Validated<DecodedBallot>> =
        ballotIds.map { DecodedBallot(it, intentionallyInvalid = false, abstained = false, tooLong = false, decodedLists = emptyList()).asMalformed }

    /**
     * Encapsulates a byte array in order to enable reading next bytes, strings, and ints from it.
     *
     * The methods return null, if the required object (byte/string/int) cannot be extracted.
     */
    class ByteSource(private val bytes: ByteArray) {
        private var index = 0

        fun empty(): Boolean = index >= bytes.size

        fun getNextString(numberOfCharacters: Int): String? = runCatching {
            getNextBytes(2 * numberOfCharacters)?.toString(Charsets.UTF_16BE)
        }.getOrNull()

        fun getNextByteAsBitSet(): BitSet? {
            val nextByte = getNextByte() ?: return null
            return BitSet.valueOf(byteArrayOf(nextByte))
        }

        fun getNextByteAsInt(): Int? {
            val nextByte = getNextByte() ?: return null
            val nextInt = nextByte.toUByte().toInt()
            check(nextInt >= 0) { "Expected positive number, found $nextInt" } // not expected to happen => throwing
            return nextInt
        }

        private fun getNextByte(): Byte? =
            if (index in bytes.indices) bytes[index++] else null

        fun getNextBytes(numberOfBytes: Int): ByteArray? =
            ByteArray(numberOfBytes) { _ -> getNextByte() ?: return null }
    }
}

internal object BitfieldPos {
    const val INTENTIONALLY_INVALID = 0
    const val ABSTAIN = 1
    const val TOO_LONG = 2
}

internal interface SingleBallotDecoder {
    val ballotId: BallotId
    fun ByteSource.decodeBallot(): DecodingResult<DecodedBallot>
    fun encodedBallotSize(): Int
}

internal fun singleBallotDecoder(ballotSpec: BallotSpec): SingleBallotDecoder =
    when (ballotSpec.encoding) {
        is BallotEncodingSpec.Standard -> StandardBallotDecoder(ballotSpec)
        is BallotEncodingSpec.Sparse -> SparseBallotDecoder(ballotSpec)
    }

/**  Decodes a public label (given as a string) into a list of ballot identifiers */
fun decodePublicLabel(publicLabel: String): List<BallotId> =
    publicLabel.split(":").map { BallotId(it) }

internal sealed interface DecodingResult<out T> {
    data class Success<T>(val value: T) : DecodingResult<T>
    enum class Error : DecodingResult<Nothing> {
        TooShort,
        DuplicatedChoiceIds,
        InvalidChoiceIndex,
    }
}

internal inline fun <T> DecodingResult<T>.ifError(block: (DecodingResult.Error) -> T): T =
    when (this) {
        is DecodingResult.Success<T> -> this.value
        is DecodingResult.Error -> block(this)
    }

internal fun <T> T.asDecodingResult() = DecodingResult.Success(this)

