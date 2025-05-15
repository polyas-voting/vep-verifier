package de.polyas.core3.vep.verif

import kotlin.test.Test

class VerifierTest {

    @Test
    fun `without revocations`() {
        val verifier = Verifier("test-data/small-no-revocations")
        verifier.performVerification()
    }

    @Test
    fun `with revocations`() {
        val verifier = Verifier("test-data/small-with-revocations")
        verifier.performVerification()
    }
}