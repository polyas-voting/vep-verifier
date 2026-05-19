/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.input

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.vep.verif.types.*
import java.io.File

/**
 * Object offering access to the content of various bulletin board, read from the appropriate files.
 */
class Input(
    baseFolder: File,
    private val receiptFolder: File? = null
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

    fun ballotFlagged(): Sequence<Packet<AnnotatedBallot>> =
        reader.sequence<WithPayload<Packet<AnnotatedBallot>>>("ballot-flagged.json")
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

    fun receipts(): Sequence<Receipt> =
        receiptFolder?.takeIf { it.exists() && it.isDirectory }
            ?.let { receiptPath ->
                (receiptPath.listFiles() ?: emptyArray())
                    .asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.extension == "pdf" }
                    .mapNotNull { file ->
                        file.pdfToText().receipt()
                            .also { if (it == null) println("  *** WARN: - No receipt found in file $file") }
                    }
            } ?: emptySequence()

    val secondDeviceParametersFingerprint: SecondDeviceParametersFingerprint? = run {
         val file = baseFolder.resolve("secondDeviceParametersFingerprint.json")
         if (file.exists()) {
             objectMapper.readValue<SecondDeviceParametersFingerprint>(file)
         } else {
             null
         }
    }

    ////

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WithPayload<T>(val payload:T)

    companion object {
        val objectMapper = jacksonObjectMapper()
    }
}

private fun String.receipt(): Receipt? {
    fun extract(beginTag: String, endTag: String): String =
        substringAfter(beginTag, "")
            .substringBefore(endTag, "")
            .filterNot { it.isWhitespace() }

    val fingerprint = extract("-----BEGIN FINGERPRINT-----", "-----END FINGERPRINT-----")
    val signature = extract("-----BEGIN SIGNATURE-----", "-----END SIGNATURE-----")
    return if (fingerprint.isNotBlank() and signature.isNotBlank())
        Receipt(fingerprint, signature)
    else
        null
}

