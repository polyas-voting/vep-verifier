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


