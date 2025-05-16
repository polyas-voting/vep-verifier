package de.polyas.core3.vep.verif

import kotlin.test.Test

class VerifierTest {

    @Test
    fun `without revocations`() {
        val baseFolder = "test-data/small-no-revocations"
        val verifier = Verifier(baseFolderName = baseFolder, receiptsFolderName = "$baseFolder/receipts")
        verifier.performVerification()
    }

    @Test
    fun `with revocations`() {
        val verifier = Verifier(baseFolderName = "test-data/small-with-revocations", receiptsFolderName = null)
        verifier.performVerification()
    }
}