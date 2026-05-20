/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.reporting

import de.polyas.core3.vep.verif.types.*

fun reportTurnout(ballots: () -> Sequence<Packet<AnnotatedBallot>>) = fmt {

    +"\nBallots of the following voters were cast and took part in tallying:\n"

    indent("  ") {
        ballots()
            .flatMap { it.values }
            .filter { it.status == AnnotatedBallot.Status.OK }
            .map { it.ballot.ballot.reference }
            .sorted()
            .forEachIndexed { index, voterId ->
                +"${(index + 1).toString().padStart(5)}.  ${voterId.padEnd(10)}"
            }
    }

    +"\nBallots of the following voters were cast, but then filtered out as revoked:\n"

    indent("  ") {
        ballots()
            .flatMap { it.values }
            .filter { it.status == AnnotatedBallot.Status.REVOKED }
            .map { it.ballot.ballot.reference }
            .sorted()
            .forEachIndexed { index, voterId ->
                +"${(index + 1).toString().padStart(5)}.  ${voterId.padEnd(10)}"
            }
    }
}


