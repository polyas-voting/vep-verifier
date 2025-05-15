package de.polyas.core3.vep.verif.types

import de.polyas.core3.crypto.elgamal.MultiCiphertext
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.shuffle.wikstroem.ShuffleZKProof

data class MixPacket(
    val proof: ShuffleZKProof<ECElement>?,
    val ciphertexts: List<MultiCiphertext<ECElement>>,
    val publicLabel: String
)
