package de.polyas.core3.vep.verif.reporting

import de.polyas.core3.vep.verif.types.*

fun reportTurnout(ballots: () -> Sequence<Packet<Annotated>>) = fmt {

    +"\nBallots of the following voters were cast and took part in tallying:\n"

    indent("  ") {
        ballots()
            .flatMap { it.values }
            .filter { it.status == Annotated.Status.OK }
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
            .filter { it.status == Annotated.Status.REVOKED }
            .map { it.ballot.ballot.reference }
            .sorted()
            .forEachIndexed { index, voterId ->
                +"${(index + 1).toString().padStart(3)}.  ${voterId.padEnd(10)}"
            }
    }
}


