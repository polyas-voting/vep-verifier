/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.verification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.std.Hashes
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.types.*
import org.bouncycastle.util.encoders.Hex

object SDParametersVerif {

    fun verify(
        registry: Registry,
        publicKey: PublicKeyWithZKP<ECElement>,
        secondDeviceParametersFingerprint: SecondDeviceParametersFingerprint
    ) {
        println("  *** INFO: The second device parameters fingerprint = ${secondDeviceParametersFingerprint.fingerprint}")

        val computedFingerprint = Hashes.sha512 { digest(secondDeviceParametersFingerprint.publicParametersJson) }
        val computedFingerprintHex = Hex.toHexString(computedFingerprint)
        check (computedFingerprintHex == secondDeviceParametersFingerprint.fingerprint)
            { "The fingerprint of the second device public parameters is incorrect" }

        val publicParameters =
            objectMapper.readValue<SecondDeviceParameters>(secondDeviceParametersFingerprint.publicParametersJson)
        val verificationKeyHex =  registry.ballotSigningKey
        val publicKeyHex = publicKey.publicKey.asHexString()
        val ballots: List<BallotSpec> = registry.ballotStructures
        val assembledPublicParameters = SecondDeviceParameters(publicKeyHex, verificationKeyHex, ballots)
        check (publicParameters == assembledPublicParameters)
            { "The given public parameters of the second device are inconsistent with the content of the bulletin boards" }
    }

    private val objectMapper = jacksonObjectMapper()
}

private data class SecondDeviceParameters(
    val publicKey: String,
    val verificationKey: String,
    val ballots: List<BallotSpec>,
)
