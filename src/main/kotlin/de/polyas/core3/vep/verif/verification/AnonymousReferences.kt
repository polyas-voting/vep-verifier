package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.*
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.crypto.std.*
import org.bouncycastle.util.encoders.Base64
import java.math.BigInteger

/**
 * Utility for calculating anonymous references.
 */
object AnonymousReferences {

    private val group = EllipticCurveInst()
    private val pedersen = Pedersen(group)

    /**
     * Computes the reference from the given [voterId] and [referenceCoin],
     * as the (hash of the) Pedersen commitment to [voterId] with the randomness [referenceCoin].
     */
    fun reference(voterId: String, referenceCoin: BigInteger): String {
        val commitment = pedersen.commit(uniformHash(voterId), referenceCoin)
        val hash = Hashes.sha256(group.asBytes(commitment))
        return Base64.toBase64String(hash).take(22).chunked(6).joinToString("-")
    }

    private fun uniformHash(s: String): BigInteger = uniformHash(group.order) {
        digest(s)
    }
}
