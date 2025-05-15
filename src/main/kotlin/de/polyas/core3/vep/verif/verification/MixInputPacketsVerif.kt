package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.Ciphertext
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.vep.verif.types.*

/**
 * Verification of the board with input mix packets.
 *
 * It checks that, for each public label, valid ballots are collected in packages,
 * preserving the original order and that the packages are of appropriate size.
 */
object MixInputPacketsVerif {

    fun verify(
        registry: Registry,
        ballotFlagged: () -> Sequence<Packet<Annotated>>,
        mixPackets: () -> Sequence<MixPacket>
    ) {
        checkOrder(registry, ballotFlagged, mixPackets)
        checkPacketLength(registry, mixPackets)
    }

    /**
     * Checks that the order of correct ballots, for each label, is preserved
     */
    private fun checkOrder(
        registry: Registry,
        ballotFlagged: () -> Sequence<Packet<Annotated>>,
        mixPackets: () -> Sequence<MixPacket>
    ) {
        for (label in publicLabels(registry)) {
            val inputStream: Sequence<List<Ciphertext<ECElement>>> =
                ballotFlagged()
                    .flatMap { it.values }
                    .filter { it.status == Annotated.Status.OK && it.ballot.ballot.publicLabel == label } // only correct ballots with the considered label
                    .map { it.ballot.ballot.encryptedChoice } // take the encrypted choice
                    .map { it.ciphertexts }

            val outputStream: Sequence<List<Ciphertext<ECElement>>> =
                mixPackets()
                    .filter { it.publicLabel == label }
                    .flatMap { it.ciphertexts.asSequence() }
                    .map { it.ciphertexts }

            check(
                equalSequences(inputStream, outputStream)
            ) { "the input mix packets do not contain the expected ciphertexts for public label $label" }
        }
    }

    /**
     * Checks the length of each mix packet
     */
    private fun checkPacketLength(
        registry: Registry,
        mixPackets: () -> Sequence<MixPacket>
    ) {
        val K = registry.packetSize
        // we will need the number of packets for every public label
        val noPacketsForLabel: Map<String, Int> = mixPackets().groupingBy(MixPacket::publicLabel).eachCount()
        for (mixPacket in mixPackets()) {
            val packetSize = mixPacket.ciphertexts.size
            // the packet size should never exceed K
            check(packetSize <= K) { "The size of the packet ($packetSize) exceeds the allowed $K" }
            // the packet size should be at least K/2, unless there is only one packet with the given label
            val n = noPacketsForLabel[mixPacket.publicLabel] ?: 0
            check(n < 2 || packetSize >= K / 2) { "The size of the packet ($packetSize) is smaller that allowed ${K / 2}" }
        }
    }

    private fun publicLabels(registry: Registry): List<String> =
        registry.voters
            .map { it.publicLabel }
            .distinct()
            .toList()
}