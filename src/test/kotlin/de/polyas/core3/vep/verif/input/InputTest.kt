/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.input

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputTest {

    @Test
    fun `read filtered out with revocations`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))

        assertTrue("Revocation board should not be empty") {
            input.ballotFilteredOut().count() > 0
        }
    }

    @Test
    fun `read filtered out without revocations`() {
        val input = Input(baseFolder = File("test-data/small-no-revocations"))

        assertTrue("Revocation board should be empty") {
            input.ballotFilteredOut().count() == 0
        }
    }

    @Test
    fun `read election key`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.electionKey
    }

    @Test
    fun `read the registry`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.registry
    }

    @Test
    fun `read revocation tokens`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.revocationTokens()?.toList()
        assertNotNull(input.revocationTokens())
    }

    @Test
    fun `assert no revocation tokens`() {
        val input = Input(baseFolder = File("test-data/small-no-revocations"))
        assertNull(input.revocationTokens())
    }

    @Test
    fun `read ballot box`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.ballotBox().toList()
    }

    @Test
    fun `read ballot flagged`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.ballotFlagged().toList()
    }

    @Test
    fun `read input mix packets`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.inputMixPackets().toList()
    }

    @Test
    fun `read output mix packet`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.outputMixPackets().toList()
    }

    @Test
    fun `read decrypted packets`() {
        val input = Input(baseFolder = File("test-data/small-with-revocations"))
        input.decryptedPackets().toList()
    }
}
