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

import de.polyas.core3.crypto.elgamal.zkp.DlogNIZKP
import de.polyas.core3.vep.verif.input.Input
import de.polyas.core3.vep.verif.types.*
import java.io.File
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class BallotFlaggedVerifTest {

    @Test
    fun `data from sample folder passes verification`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))

        BallotFlaggedVerif(data.registry, anonymousRevocations = true).verify(
            ballotBox = data::ballotBox,
            ballotFlagged = data::ballotFlagged,
            ek = data.electionKey,
            revocationTokens = data::revocationTokens
        )
    }

    @Test
    fun `skipped entry causes failure`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))
        val ballots = data.ballotBox().toList()
        val modifiedBallots = ballots + ballots[0]

        expectFailureWith("the number of input and output elements is not the same") {
            BallotFlaggedVerif(data.registry, anonymousRevocations = true).verify(
                ballotBox = modifiedBallots.asLazySequence(),
                ballotFlagged = data::ballotFlagged,
                ek = data.electionKey,
                revocationTokens = data::revocationTokens
            )
        }
    }

    @Test
    fun `duplicated entry not flagged causes failure`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))

        val ballots = data.ballotBox().toList()
        val modifiedBallots = ballots + ballots[0]
        val flaggedBallots = data.ballotFlagged().toList()
        val modifiedFlaggedBallots = flaggedBallots + flaggedBallots[0]

        expectFailureWith("Duplicated ballot") {
            BallotFlaggedVerif(data.registry, anonymousRevocations = true).verify(
                ballotBox = modifiedBallots.asLazySequence() ,
                ballotFlagged = modifiedFlaggedBallots.asLazySequence(),
                ek = data.electionKey,
                revocationTokens = data::revocationTokens
            )
        }
    }

    @Test
    fun `duplicated entry is okay if it is flagged`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))

        // we add an extra ballot to the input ballots
        val ballots = data.ballotBox().toList()
        val extraBallot = ballots.first().ballots.first()
        val modifiedBallots = ballots + BallotPackage(listOf(extraBallot))

        // we also add an extra ballot to the flagged ballots, but mark is as INCORRECT
        val flaggedBallots = data.ballotFlagged().toList()
        val extraFlagged = flaggedBallots.first().values.first().copy(
            status = AnnotatedBallot.Status.INCORRECT
        )
        val modifiedFlaggedBallots = flaggedBallots + Packet(listOf(extraFlagged))

        BallotFlaggedVerif(data.registry, anonymousRevocations = true).verify(
            ballotBox = modifiedBallots.asLazySequence(),
            ballotFlagged = modifiedFlaggedBallots.asLazySequence(),
            ek = data.electionKey,
            revocationTokens = data::revocationTokens
        )
    }

    @Test
    fun `mismatched public label not labeled as incorrect`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))

        val modifiedRegistry = data.registry.copy(
            voters = data.registry.voters.map { voter ->
                voter.copy(publicLabel = "x")
            }
        )

        expectFailureWith("mismatched public label") {
            BallotFlaggedVerif(modifiedRegistry, anonymousRevocations = true).verify(
                ballotBox = data::ballotBox,
                ballotFlagged = data::ballotFlagged,
                ek = data.electionKey,
                revocationTokens = data::revocationTokens
            )
        }
    }

    @Test
    fun `mismatched public labels are okay if flagged`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))

        val modifiedRegistry = data.registry.copy(
            voters = data.registry.voters.map { voter ->
                voter.copy(publicLabel = "x")
            }
        )

        val modifiedFlagged = data.ballotFlagged().toList().deepMap { it.copy(status = AnnotatedBallot.Status.INCORRECT) }

        BallotFlaggedVerif(modifiedRegistry, anonymousRevocations = true).verify(
            ballotBox = data::ballotBox,
            ballotFlagged = modifiedFlagged.asLazySequence(),
            ek = data.electionKey,
            revocationTokens = data::revocationTokens
        )
    }

    @Test
    fun `invalid zkp not flagged as incorrect`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))

        val modifiedBallots = data.ballotBox().toList().deepBallotMap {
           it.copy(
               ballot = it.ballot.copy(zkp = it.ballot.zkp.map { it.deterministicallyScrew() })
           )
        }
        val modifiedFlagged = data.ballotFlagged().toList().deepMap {
            it.copy(
                ballot = it.ballot.copy(
                    ballot = it.ballot.ballot.copy(zkp = it.ballot.ballot.zkp.map { it.deterministicallyScrew() })
                )
            )
        }

        expectFailureWith("invalid zero-knowledge proof") {
            BallotFlaggedVerif(data.registry, anonymousRevocations = true).verify(
                ballotBox = modifiedBallots.asLazySequence(),
                ballotFlagged = modifiedFlagged.asLazySequence(),
                ek = data.electionKey,
                revocationTokens = data::revocationTokens
            )
        }
    }

    @Test
    fun `invalid signature not flagged as incorrect`() {
        val data = Input(baseFolder = File("test-data/small-with-revocations"))

        val modifiedBallots = data.ballotBox().toList().deepBallotMap {
            it.copy(
                ballot = it.ballot.copy(signature = it.ballot.signature.deterministicallyScrew())
            )
        }
        val modifiedFlagged = data.ballotFlagged().toList().deepMap {
            it.copy(
                ballot = it.ballot.copy(
                    ballot = it.ballot.ballot.copy(signature = it.ballot.ballot.signature.deterministicallyScrew())
                )
            )
        }

        expectFailureWith("invalid zero-knowledge proof") {
            BallotFlaggedVerif(data.registry, anonymousRevocations = true).verify(
                ballotBox = modifiedBallots.asLazySequence(),
                ballotFlagged = modifiedFlagged.asLazySequence(),
                ek = data.electionKey,
                revocationTokens = data::revocationTokens
            )
        }
    }

    @Test
    fun `revoked ballots should be marked as such`() {
        val data = Input(baseFolder = File("test-data/small-no-revocations"))

        val registry = data.registry
        val revokedVoterIds = registry.voters
                .map { it.id }
                .filter { it.first().isDigit() } // select some
                .toList()
        val revocationTokens = revokedVoterIds.map { RevocationToken("election-id", listOf(it)) }

        expectFailureWith("should be marked as revoked") {
            BallotFlaggedVerif(registry, anonymousRevocations = true).verify(
                ballotBox = data::ballotBox,
                ballotFlagged = data::ballotFlagged,
                ek = data.electionKey,
                revocationTokens = revocationTokens.asLazySequence()
            )
        }
    }
}

private fun Iterable<BallotPackage>.deepBallotMap(f: (BallotEntry) -> BallotEntry): List<BallotPackage> =
    this.map {
        it.copy(ballots = it.ballots.map(f))
    }

private fun Iterable<Packet<AnnotatedBallot>>.deepMap(f: (AnnotatedBallot) -> AnnotatedBallot): List<Packet<AnnotatedBallot>> =
    this.map {
        it.copy(values = it.values.map(f))
    }

private fun <T> Iterable<T>.asLazySequence(): (() -> Sequence<T>) =
    { this.asSequence()}


private fun expectFailureWith(messageIncluding: String, block: () -> Unit) {
    try {
        block()
        fail("Expected failure containing message: `$messageIncluding`")
    } catch (e: IllegalStateException) {
        assertEquals(true, e.message?.contains(messageIncluding),
            "Expected failure containing message: `$messageIncluding`. Got ${e.message}")
    }
}

private fun DlogNIZKP.Proof.deterministicallyScrew(): DlogNIZKP.Proof {
    return this.copy(f = BigInteger.TEN)
}

