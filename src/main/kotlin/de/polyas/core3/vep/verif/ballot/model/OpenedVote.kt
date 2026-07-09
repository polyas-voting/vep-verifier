/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot.model

/** Decrypted individual ballot, containing decrypted voter's plaintext choice */
data class OpenedVote(
    /** Raw bytes of the decrypted voter's choice, as cast (not decoded nor interpreted), represented as a hexadecimal string */
    val ballot: String,

    /** Voter's public label defining the ballot sheets this voter is voting for */
    val publicLabel: String,
)
