package de.polyas.core3.vep.verif.types

data class RevocationToken(
    val electionId: String,
    val voterIds: List<String>
)
