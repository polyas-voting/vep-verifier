package de.polyas.core3.vep.verif.ballot.decoding

import kotlin.test.*

class SparseBallotDecodingTest {

    @Test
    fun `empty byte array`() {
        val ballotSpec = ballot(maxVotes = 2, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(maxVotesForList = 0, maxVotesOnList = 6, maxVotesTotal = null) {
                candidate(maxVotes = 2)
                candidate(maxVotes = 1)
            }
        }.withSparseEncoding(byttesPerChoice = 1, encodedBallotSize = 10)
        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)

        val ballotHex = ""
        val decoded = decoder.decodeBallot(ballotHex)
        assertEquals(DecodingResult.Error.TooShort, decoded)
    }

    @Test
    fun `blank ballot`() {
        val ballotHex = "00".repeat(5)
        val encodingSize = ballotHex.length/2
        val ballotSpec = ballot(maxVotes = 2, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "l1", maxVotesForList = 0, maxVotesOnList = 6, maxVotesTotal = null) {
                candidate(id = "c1", maxVotes = 2)
                candidate(id = "c2", maxVotes = 1)
            }
        }.withSparseEncoding(encodedBallotSize = encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)
        val decoded = decoder.decodeBallot(ballotHex).assertSuccess()

        // all should be zero and false flags
        assertFalse(decoded.intentionallyInvalid)
        assertFalse(decoded.abstained)
        for (decodedList in decoded.decodedLists) {
            expect(0) { decodedList.votes }
            for (decodedCandidate in decodedList.decodedCandidates) {
                expect(0) { decodedCandidate.votes }
            }
        }
    }

    @Test
    fun `ballot with choices`() {
        val ballotHex = "02" + "0105" + "0201" + "0403" + "0702" + "0000"
        val encodingSize = ballotHex.length/2
        val ballotSpec = ballot(maxVotes = 5, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "A", maxVotesForList = 1, maxVotesOnList = 3, maxVotesTotal = null) { // #1
                candidate(id = "A1", maxVotes = 2) // #2
                candidate(id = "A2", maxVotes = 2) // #3
                candidate(id = "A3", maxVotes = 2) // #4
                candidate(id = "A4", maxVotes = 2) // #5
            }

            list(id = "B", maxVotesForList = 0, maxVotesOnList = 2, maxVotesTotal = null) { // --
                candidate(id = "B1", maxVotes = 2) // #6
                candidate(id = "B2", maxVotes = 2) // #7
                candidate(id = "B3", maxVotes = 2) // #8
            }
        }.withSparseEncoding(encodedBallotSize = encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)
        val decoded = decoder.decodeBallot(ballotHex).assertSuccess()

        assertFalse(decoded.intentionallyInvalid)
        assertTrue(decoded.abstained)
        val choiceMap = choiceMap(decoded)
        expect(5) { choiceMap["A"]?.votes }
        expect(1) { choiceMap["A1"]?.votes }
        expect(0) { choiceMap["A2"]?.votes }
        expect(3) { choiceMap["A3"]?.votes }
        expect(0) { choiceMap["A4"]?.votes }
        expect(0) { choiceMap["B1"]?.votes }
        expect(2) { choiceMap["B2"]?.votes }
        expect(0) { choiceMap["B3"]?.votes }
    }

    @Test
    fun `ballot with max votes`() {
        val ballotHex = "00" + "01ff" + "02ff"
        val encodingSize = ballotHex.length/2
        val ballotSpec = ballot(maxVotes = 255, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "A", maxVotesForList = 0, maxVotesOnList = 255, maxVotesTotal = null) { // #1
                candidate(id = "A1", maxVotes = 255) // #1
                candidate(id = "A2", maxVotes = 255) // #2
            }
        }.withSparseEncoding(encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)
        val decoded = decoder.decodeBallot(ballotHex).assertSuccess()

        assertFalse(decoded.intentionallyInvalid)
        assertFalse(decoded.abstained)
        val choiceMap = choiceMap(decoded)
        expect(0) { choiceMap["A"]?.votes }
        expect(255) { choiceMap["A1"]?.votes }
        expect(255) { choiceMap["A2"]?.votes }
    }

    @Test
    fun `multiple ballots in one array`() {
        val ballotHex = "02" + "0105" + "0201" + "0403" + "0702" + "0000"
        val encodingSize = ballotHex.length/2
        val ballotSpec = ballot(maxVotes = 5, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "A", maxVotesForList = 1, maxVotesOnList = 3, maxVotesTotal = null) { // #1
                candidate(id = "A1", maxVotes = 2) // #2
                candidate(id = "A2", maxVotes = 2) // #3
                candidate(id = "A3", maxVotes = 2) // #4
                candidate(id = "A4", maxVotes = 2) // #5
            }

            list(id = "B", maxVotesForList = 0, maxVotesOnList = 2, maxVotesTotal = null) { // --
                candidate(id = "B1", maxVotes = 2) // #6
                candidate(id = "B2", maxVotes = 2) // #7
                candidate(id = "B3", maxVotes = 2) // #8
            }
        }.withSparseEncoding(encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)
        val byteSource = BallotDecoder.ByteSource(ballotHex.repeat(2).hexToByteArray())

        with(decoder) {
            val decoded1 = byteSource.decodeBallot()
            val decoded2 = byteSource.decodeBallot()
            assertNotNull(decoded1)
            assertEquals(decoded1, decoded2)
            val decoded3 = byteSource.decodeBallot()
            assertEquals(DecodingResult.Error.TooShort, decoded3)
        }
    }

    @Test
    fun `ballot with duplicated IDs`() {
        val ballotHex = "02" + "0105" + "0201" + "0203" + "0702" + "0000"
        val encodingSize = ballotHex.length/2
        val ballotSpec = ballot(maxVotes = 5, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "A", maxVotesForList = 1, maxVotesOnList = 3, maxVotesTotal = null) { // #1
                candidate(id = "A1", maxVotes = 2) // #2
                candidate(id = "A2", maxVotes = 2) // #3
                candidate(id = "A3", maxVotes = 2) // #4
                candidate(id = "A4", maxVotes = 2) // #5
            }

            list(id = "B", maxVotesForList = 0, maxVotesOnList = 2, maxVotesTotal = null) { // --
                candidate(id = "B1", maxVotes = 2) // #6
                candidate(id = "B2", maxVotes = 2) // #7
                candidate(id = "B3", maxVotes = 2) // #8
            }
        }.withSparseEncoding(encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)

        val decoded = decoder.decodeBallot(ballotHex)
        assertEquals(DecodingResult.Error.DuplicatedChoiceIds, decoded) // malformed should result in null
    }

    @Test
    fun `ballot with choices, full`() {
        val ballotHex = "02" + "0105" + "0201" + "0403" + "0702" + "0801"
        val encodingSize = ballotHex.length/2
        val ballotSpec = ballot(maxVotes = 5, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "A", maxVotesForList = 1, maxVotesOnList = 3, maxVotesTotal = null) { // #1
                candidate(id = "A1", maxVotes = 2) // #2
                candidate(id = "A2", maxVotes = 2) // #3
                candidate(id = "A3", maxVotes = 2) // #4
                candidate(id = "A4", maxVotes = 2) // #5
            }

            list(id = "B", maxVotesForList = 0, maxVotesOnList = 2, maxVotesTotal = null) { // --
                candidate(id = "B1", maxVotes = 2) // #6
                candidate(id = "B2", maxVotes = 2) // #7
                candidate(id = "B3", maxVotes = 2) // #8
            }
        }.withSparseEncoding(encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)

        val decoded = decoder.decodeBallot(ballotHex).assertSuccess()

        assertFalse(decoded.intentionallyInvalid)
        assertTrue(decoded.abstained)
        val choiceMap = choiceMap(decoded)
        expect(5) { choiceMap["A"]?.votes }
        expect(1) { choiceMap["A1"]?.votes }
        expect(0) { choiceMap["A2"]?.votes }
        expect(3) { choiceMap["A3"]?.votes }
        expect(0) { choiceMap["A4"]?.votes }
        expect(0) { choiceMap["B1"]?.votes }
        expect(2) { choiceMap["B2"]?.votes }
        expect(1) { choiceMap["B3"]?.votes }
    }

    @Test
    fun `ballot with write-ins`() {
        val writeInContent4Characters = "bóx ".toByteArray(Charsets.UTF_16BE)
        expect(8) { writeInContent4Characters.size }
        val ballotHex = "02" + "0105" +
                "02" + writeInContent4Characters.toHexString() + "01" +
                "00".repeat(6) // 1 + 2*2 + 1
        val encodingSize = ballotHex.length/2

        val ballotSpec = ballot(maxVotes = 5, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "A", maxVotesForList = 0, maxVotesOnList = 3, maxVotesTotal = null) {
                candidate(id = "A1", maxVotes = 2) // #1
                candidate(id = "A2", maxVotes = 2, writeInSize = 4) // #2
                candidate(id = "A3", maxVotes = 2, writeInSize = 2) // #3
            }
        }.withSparseEncoding(encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)

        val decoded = decoder.decodeBallot(ballotHex).assertSuccess()

        assertFalse(decoded.intentionallyInvalid)
        assertTrue(decoded.abstained)
        val choiceMap = choiceMap(decoded)
        expect(0) { choiceMap["A"]?.votes }
        expect(5) { choiceMap["A1"]?.votes }
        expect(1) { choiceMap["A2"]?.votes }
        expect("bóx ") { choiceMap["A2"]?.writeIn }
        expect(0) { choiceMap["A3"]?.votes }
        expect(null) { choiceMap["A3"]?.writeIn }
    }

    @Test
    fun `ballot with write-ins, full`() {
        val writeInContent4Characters = "bóx ".toByteArray(Charsets.UTF_16BE)
        expect(8) { writeInContent4Characters.size }
        val writeInContent2Characters = "üE".toByteArray(Charsets.UTF_16BE)
        expect(4) { writeInContent2Characters.size }
        val ballotHex = "02" + "0105" +
                "02" + writeInContent4Characters.toHexString() + "01" +
                "03" + writeInContent2Characters.toHexString() + "03"
        val encodingSize = ballotHex.length/2

        val ballotSpec = ballot(maxVotes = 5, maxVotesForCandidates = null, maxVotesForLists = null) {
            list(id = "A", maxVotesForList = 0, maxVotesOnList = 3, maxVotesTotal = null) {
                candidate(id = "A1", maxVotes = 2) // #1
                candidate(id = "A2", maxVotes = 2, writeInSize = 4) // #2
                candidate(id = "A3", maxVotes = 2, writeInSize = 2) // #3
            }
        }.withSparseEncoding(encodingSize)

        val decoder = singleBallotDecoder(ballotSpec)
        assertIs<SparseBallotDecoder>(decoder)

        val decoded = decoder.decodeBallot(ballotHex).assertSuccess()

        assertFalse(decoded.intentionallyInvalid)
        assertTrue(decoded.abstained)
        val choiceMap = choiceMap(decoded)
        expect(0) { choiceMap["A"]?.votes }
        expect(5) { choiceMap["A1"]?.votes }
        expect(1) { choiceMap["A2"]?.votes }
        expect("bóx ") { choiceMap["A2"]?.writeIn }
        expect(3) { choiceMap["A3"]?.votes }
        expect("üE") { choiceMap["A3"]?.writeIn }
    }
}

private data class ChoiceMapItem(
    val votes: Int,
    val writeIn: String?
)

private fun choiceMap(decodedBallot: DecodedBallot): Map<String, ChoiceMapItem> =
    buildMap {
        for (decodedList in decodedBallot.decodedLists) {
            put(decodedList.id, ChoiceMapItem(decodedList.votes, null))
            for (decodedCandidate in decodedList.decodedCandidates) {
                put(decodedCandidate.id, ChoiceMapItem(decodedCandidate.votes, decodedCandidate.writeInContent))
            }
        }
    }
