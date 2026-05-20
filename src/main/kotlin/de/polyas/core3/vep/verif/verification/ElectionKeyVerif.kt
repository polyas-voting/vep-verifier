/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.verification

import de.polyas.core3.crypto.elgamal.VerificationResult
import de.polyas.core3.crypto.elgamal.instance.ECElement
import de.polyas.core3.crypto.elgamal.instance.EllipticCurveInst
import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP
import de.polyas.core3.vep.verif.types.PublicKeyWithZKP

/**
 * Verification of the election key
 */
object ElectionKeyVerif {
    private val group = EllipticCurveInst()

    fun verify(ek: PublicKeyWithZKP<ECElement>) {
        // check tha the entry contains valid zero-knowledge proof
        val result = DlogNIZKP(group).verify(ek.publicKey, ek.zkp)
        check (result is VerificationResult.Correct) {
            "Incorrect zero-knowledge proof in the public election key: $result"
        }
    }
}

