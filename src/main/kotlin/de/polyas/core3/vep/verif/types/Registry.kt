/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.types

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.vep.verif.ballot.model.BallotSpec

/**
 * Registry data, as published on the registration board
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Registry(
    val packetSize: Int,
    val electionId: String,
    val ballotStructures: List<BallotSpec>,
    val voters: List<Voter>,
    val voterIdentifiers: List<PublicVoterId>,
    val ballotSigningKey: String
)

data class Voter(
    val id: String,
    val publicLabel: String,
    val cred: ECElement,
    val aux: Map<String, String>?
)

data class PublicVoterId(
    val publicId: String,
    val publicLabel: String
)
