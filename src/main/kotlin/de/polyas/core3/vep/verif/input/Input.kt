package de.polyas.core3.vep.verif.input

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.vep.verif.types.*
import java.io.File

/**
 * Object offering access to the content of various bulletin board, read from the appropriate files.
 */
class Input(
    baseFolder: File,
    // private val receiptFolder: File? = null
) {

    private val reader = JsonFileReader(baseFolder)

    val electionKey: PublicKeyWithZKP<ECElement> by lazy {
        reader.singleton("keygen-electionKey-Polyas.json")
    }

    val registry: Registry by lazy {
        reader.singleton("registry.json")
    }

    fun revocationTokens(): Sequence<RevocationToken>? =
        reader.sequenceOrNull<WithPayload<RevocationToken>>("revocations.json")
            ?.map { it.payload }
        // Note: the pattern used here and in the remaining methods, with explicit mapping to it.payload
        // is used to circumvent some limitations of the type inference with reified types.

    fun ballotBox(): Sequence<BallotPackage> =
        reader.sequence<WithPayload<BallotPackage>>("ballot-box.json")
            .map { it.payload }

    fun ballotFlagged(): Sequence<Packet<Annotated>> =
        reader.sequence<WithPayload<Packet<Annotated>>>("ballot-flagged.json")
            .map { it.payload }

    fun ballotFilteredOut(): Sequence<String> {
        return reader.sequence<WithPayload<String>>("ballot-filtered-out.json")
            .map { it.payload }
    }

    fun inputMixPackets(): Sequence<MixPacket> =
        reader.sequence<WithPayload<MixPacket>>("mixing-input-packets.json")
            .map { it.payload }

    fun outputMixPackets(): Sequence<MixPacket> =
        reader.sequence<WithPayload<MixPacket>>("mixing-mix-Polyas.json")
            .map { it.payload }

    fun decryptedPackets(): Sequence<MessageWithProofPacket> =
        reader.sequence<WithPayload<MessageWithProofPacket>>("decryption-decrypt-Polyas.json")
            .map { it.payload }


    ////

    // fun receipts(): Sequence<Receipt> =
    //     receiptFolder?.takeIf { it.exists() && it.isDirectory}
    //             ?.let { receiptPath ->
    //                 (receiptPath.listFiles() ?: emptyArray())
    //                     .filter { !it.isDirectory }
    //                     .filter { it.extension == "pdf" }
    //                     .mapNotNull { file ->
    //                         file.pdfToText().receipt()
    //                             .also { if (it == null) println("  *** WARN: - No receipt found in file $file") }
    //                     }
    //                     .asSequence()
    //             } ?: emptySequence()

    // val secondDeviceParametersFingerprint: SecondDeviceParametersFingerprint? = run {
    //     val file = baseFolder.resolve("secondDeviceParametersFingerprint.json")
    //     if (file.exists()) {
    //         customizedObjectMapper.readValue<SecondDeviceParametersFingerprint>(file)
    //     } else {
    //         null
    //     }
    // }

    ////

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WithPayload<T>(val payload:T)
}

