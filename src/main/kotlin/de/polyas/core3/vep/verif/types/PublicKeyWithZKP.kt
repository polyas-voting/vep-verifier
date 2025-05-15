package de.polyas.core3.vep.verif.types

import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP

data class PublicKeyWithZKP<GroupElem>(
    val publicKey: GroupElem,
    val zkp: DlogNIZKP.Proof
)