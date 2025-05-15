package de.polyas.core3.vep.verif.types

import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.vep.verif.crypto.VerifiableEPBallotAlgo

data class BallotEntry(
    val ballot: VerifiableEPBallotAlgo.Ballot<ECElement>,
)
