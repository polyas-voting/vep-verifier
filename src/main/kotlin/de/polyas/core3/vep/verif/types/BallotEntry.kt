package de.polyas.core3.vep.verif.types

import de.polyas.core3.crypto.elgamal.instance.ECElement

data class BallotEntry(
    val ballot: Ballot<ECElement>,
)
