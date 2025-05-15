package de.polyas.core3.vep.verif.types

data class Annotated(
    val ballot: BallotEntry,
    val status: Status,
    val annotation: String
) {
    enum class Status { OK, INCORRECT, REVOKED }
}
