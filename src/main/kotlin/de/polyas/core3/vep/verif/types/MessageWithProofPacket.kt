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

import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.zkp.DecryptionZKP
import de.polyas.core3.crypto.std.Message

data class MessageWithProofPacket (
    val publicLabel: String,
    val messagesWithZKP: List<MessageWithProof<ECElement>>
) {
    data class MessageWithProof<G>(
        val message: Message,
        val auxData: Map<String, String>?,
        val proof: List<DecryptionZKP.Proof<G>>
    )
}
