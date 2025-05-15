package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.MultiCommitmentKey.Companion.generateVerifiably
import de.polyas.core3.crypto.elgamal.VerificationResult
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.crypto.elgamal.shuffle.wikstroem.Shuffle
import de.polyas.core3.vep.verif.types.MixPacket
import de.polyas.core3.vep.verif.types.PublicKeyWithZKP
import de.polyas.core3.vep.verif.types.Registry

/**
 * Verification of the shuffle output.
 */
object MixOutputPacketsVerif {

    private val group = EllipticCurveInst()

    fun verify(
        registry: Registry,
        ek: PublicKeyWithZKP<ECElement>,
        inputPackets: ()-> Sequence<MixPacket>,
        outputPackets: ()-> Sequence<MixPacket>
    ) {
        val ck = generateVerifiably(group, registry.packetSize, "Polyas")

        applyToCorresponding(inputPackets(), outputPackets()) { inputPacket, outputPacket ->
            check(inputPacket.publicLabel == outputPacket.publicLabel) {"the public labels do not match"}
            check(outputPacket.proof != null) {"Zero knowledge proof is missing"}

            val shuffle = Shuffle(group, ek.publicKey, ck)
            val result = shuffle.checkProof(
                ic = inputPacket.ciphertexts,
                oc = outputPacket.ciphertexts,
                proof = outputPacket.proof
            )
            check (result is VerificationResult.Correct) {
                "Incorrect zero-knowledge proof of shuffle: $result"
            }
        }
    }
}

