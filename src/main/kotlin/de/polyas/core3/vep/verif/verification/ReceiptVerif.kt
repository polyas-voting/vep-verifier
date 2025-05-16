package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.crypto.std.Hashes
import de.polyas.core3.crypto.std.buildByteArray
import de.polyas.core3.vep.verif.types.Ballot
import de.polyas.core3.vep.verif.types.BallotPackage
import de.polyas.core3.vep.verif.types.Receipt
import de.polyas.core3.vep.verif.types.Registry
import org.bouncycastle.util.encoders.Hex
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object ReceiptVerif {

    private val group = EllipticCurveInst()

    fun verify(registry: Registry, ballotPackages: () -> Sequence<BallotPackage>, receiptsSupplier: () -> Sequence<Receipt>) {
        println("  *** INFO: checking receipts ...")

        val signingKey = registry.ballotSigningKey
        val ks = X509EncodedKeySpec(signingKey.hexAsByteArray())
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(ks)
        val receipts = receiptsSupplier().toList()

        val castBallots = ballotPackages()
            .flatMap { it.ballots }
            .map { it.ballot.toFingerprint() }
            .toSet()

        receipts.forEach { receipt ->
            println("    checking fingerprint ${receipt.fingerprint}")
            if (!verify(publicKey, receipt.signature.hexAsByteArray(), receipt.fingerprint.hexAsByteArray())) {
                println("  *** WARN: - Incorrect signature of receipt and will therefore be ignored: $receipt")
            } else {
                check(receipt.fingerprint.lowercase() in castBallots) {
                    "Ballot correctly signed but not in ballot box: Failed to verify receipt $receipt"
                }
            }
        }
        println("  *** INFO: ${receipts.count()} receipts checked successfully. All have correct signature and are in ballot box.")
    }

    private fun verify(publicKey: PublicKey, signature: ByteArray, message: ByteArray): Boolean {
        return try {
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(message)
            sig.verify(signature)
        } catch (ex: GeneralSecurityException) {
            throw RuntimeException("Error occurred during checking a signature", ex)
        }
    }

    private fun Ballot<ECElement>.toFingerprint(): String {
        val bytes = this.toByteArray()
        val fingerprintBytes = Hashes.sha256 { digest(bytes) }
        return Hex.toHexString(fingerprintBytes)
    }

    private fun Ballot<ECElement>.toByteArray(): ByteArray = buildByteArray {
        put(encryptedChoice.ciphertexts.size)
        encryptedChoice.ciphertexts.forEach { (x, y) ->
            putWithLength(group.asBytes(x))
            putWithLength(group.asBytes(y))
        }

        putWithLength(publicLabel)
        putWithLength(reference)

        put(zkp.size)
        zkp.forEach { (c, f) ->
            putWithLength(c.toByteArray())
            putWithLength(f.toByteArray())
        }

        putWithLength(signature.c.toByteArray())
        putWithLength(signature.f.toByteArray())
    }
}

private fun String.hexAsByteArray() = this.chunked(2).map { it.lowercase().toInt(16).toByte() }.toByteArray()