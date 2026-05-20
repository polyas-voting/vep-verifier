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

@JsonIgnoreProperties(ignoreUnknown = true)
data class BallotSpec(
    val id: String,
    val contentAbove: AnyContent?,
    val title: I18nString,
    val lists: List<CandidateList>,
    val contentBelow: AnyContent?, // TextContent?,
    val showInvalidOption: Boolean,
    val showAbstainOption: Boolean,
    val maxVotes: Int,
    val minVotes: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CandidateList(
    val id: String,
    val contentAbove:  AnyContent?,
    val title: I18nString?,
    val columnHeaders: List<I18nString>,
    val candidates: List<CandidateSpec>,
    val maxVotesOnList: Int,
    val minVotesOnList: Int,
    val maxVotesForList: Int,
    val minVotesForList: Int
)

data class CandidateSpec(
    val id: String,
    val columns: List<AnyContent>,
    val maxVotes: Int,
    val minVotes: Int
)
