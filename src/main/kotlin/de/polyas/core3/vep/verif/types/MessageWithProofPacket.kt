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
