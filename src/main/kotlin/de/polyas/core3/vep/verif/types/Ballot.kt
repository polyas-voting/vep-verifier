package de.polyas.core3.vep.verif.types

import de.polyas.core3.crypto.elgamal.MultiCiphertext
import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP

data class Ballot<GroupElem>(
    val encryptedChoice: MultiCiphertext<GroupElem>,
    val zkp: List<DlogNIZKP.Proof>,
    val publicLabel: String,
    val reference: String,
    val signature: DlogNIZKP.Proof
)