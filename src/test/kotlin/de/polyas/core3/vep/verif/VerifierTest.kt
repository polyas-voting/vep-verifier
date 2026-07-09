/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif

import kotlin.test.Test

class VerifierTest {

    @Test
    fun `without revocations`() {
        val baseFolder = "test-data/small-no-revocations"
        val verifier = Verifier(baseFolderName = baseFolder, receiptsFolderName = "$baseFolder/receipts", anonymousRevocations = false)
        verifier.performVerification()
    }

    @Test
    fun `with revocations`() {
        val verifier = Verifier(baseFolderName = "test-data/small-with-revocations", receiptsFolderName = null, anonymousRevocations = true)
        verifier.performVerification()
    }

    @Test
    fun `with sparse encoding`() {
        val path = "test-data/small-with-sparse-encoding"
        val verifier = Verifier(baseFolderName = path, receiptsFolderName = null, anonymousRevocations = true)
        verifier.performVerification()
    }
}