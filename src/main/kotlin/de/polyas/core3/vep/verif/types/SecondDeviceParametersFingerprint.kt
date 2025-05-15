package de.polyas.core3.vep.verif.types

typealias JsonString = String

data class SecondDeviceParametersFingerprint(
    val publicParametersJson: JsonString,
    val fingerprint: String,
)
