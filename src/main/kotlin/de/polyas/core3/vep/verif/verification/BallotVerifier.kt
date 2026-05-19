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

import de.polyas.core3.crypto.elgamal.*
import de.polyas.core3.crypto.elgamal.VerificationResult.Correct
import de.polyas.core3.crypto.elgamal.VerificationResult.Failed
import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP
import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP.Challenger
import de.polyas.core3.crypto.std.initialDigestSha512
import de.polyas.core3.vep.verif.types.Ballot

class BallotVerifier<GroupElem>(private val group: CyclicGroup<GroupElem>) {

    /**
     * Checks correctness of the zero-knowledge proofs and the signature
     * included in the [ballot].
     *
     * This check intended to be executed both by the election controller
     * before the ballot is published and by the universal verification procedure.
     *
     * @param publicKey The election (encryption) public key
     * @param ballot The ballot containing the encrypted choice, the required zero-knowledge proof and signature.
     */
    fun verifyZKP(publicKey: GroupElem, publicCredential: GroupElem, ballot: Ballot<GroupElem>): VerificationResult {

        if (ballot.zkp.size != ballot.encryptedChoice.size())
            return Failed("ZKP of knowledge of encryption coins has invalid length")

        val dlogZKP = DlogNIZKP(group, ballotChallenger(publicKey, ballot.publicLabel, ballot.reference, ballot.encryptedChoice))

        for (i in 0 until ballot.encryptedChoice.size()) {
            dlogZKP.verify(ballot.encryptedChoice[i].x, ballot.zkp[i]) onFailure { return it }
        }

        dlogZKP.verify(publicCredential, ballot.signature) onFailure { return Failed("Invalid signature: ${it.errorMessage}") }

        return Correct
    }

    private fun ballotChallenger(
        publicKey: GroupElem, publicLabel: String, ref: String, ciphertext: MultiCiphertext<GroupElem>
    ): Challenger<GroupElem> {

        val initial = initialDigestSha512 {
            digest(group.asBytes(group.generator))
            digest(group.asBytes(publicKey))
            digest(publicLabel)
            digest(ref)

            for ((x, y) in ciphertext.ciphertexts) {
                digest(group.asBytes(x))
                digest(group.asBytes(y))
            }
        }

        return Challenger { _: GroupElem, initialZKPMessage: GroupElem ->
            initial.continueUniformHash(group.order) {
                digest(group.asBytes(initialZKPMessage))
            }
        }
    }
}
