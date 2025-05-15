package de.polyas.core3.vep.verif.reporting

import de.polyas.core3.vep.verif.types.*

fun showRegistry(registry: Registry) {

    fun showBallotIds(enc: String) : String =
        enc.split(":").joinToString{ it }

    fmt {
        +""
        +"REGISTRY:"
        +""
        indent {
            underline {
                +"anonymized voter id       | public credential                                                  | ballots "
            }
            for (voter in registry.voters) {
                + "${voter.id} | ${voter.cred} | ${showBallotIds(voter.publicLabel)}"
            }
        }
        +""
        +"PUBLIC VOTER IDENTIFIERS"
        +""
        indent {
            underline {
                +"public voter ID | public label "
            }
            for (publicId in registry.voterIdentifiers) {
                +" ${publicId.publicId} | ${publicId.publicLabel} "
            }
        }
        +""
        +""
    }
}

fun showBallots(ballots: List<BallotSpec>) {
    fmt {
        +""
        +"BALLOTS:"
        +""
        indent {
            for (ballot in ballots) {
                show(ballot)
                +""
            }
        }
    }
}

private fun Fmt.show(ballot: BallotSpec) {
    framed(header = "${ballot.title[defaultLang]} [id = ${ballot.id}]") {
        + ""
        between("[ ", " ]") {
            if (ballot.minVotes > 0) {
                +"minimum number of votes on the whole ballot = ${ballot.minVotes}"
            }
            + "maximum number of votes on the whole ballot = ${ballot.maxVotes}"
        }
        + ""

        ballot.contentAbove ?.let {
            formatted(width = 60, it.asPlainString(defaultLang))
            + ""
        }

        for (list in ballot.lists) {
            show(list)
            + ""
        }

        ballot.contentBelow ?.let {
            + it.asPlainString(defaultLang)
            + ""
        }

        if (ballot.showInvalidOption) {
            + "${boxes(1)} Mark the ballot as invalid"
        }

        if (ballot.showInvalidOption) {
            + "${boxes(1)} Mark the ballot as abstention"
        }
    }
}

private fun Fmt.show(list: CandidateList) {

    val title = list.title?.get(defaultLang) ?: "(no title)"

    framed("${boxes(list.maxVotesForList)} $title [id = ${list.id}]") {
        + ""
        between("[ ", " ]") {
           if (list.minVotesForList > 0) {
               +"minimum number of votes for the list (boxes above) = ${list.minVotesForList}"
           }
           if (list.minVotesOnList > 0) {
               +"minimum number of votes for choices/candidates on the list  = ${list.minVotesOnList}"
           }
           +"maximum number of votes for choices/candidates on the list  = ${list.maxVotesOnList}"
        }
        + ""

        list.contentAbove?. let {
            + it.asPlainString(defaultLang)
            + ""
        }


        underline {
            + list.columnHeaders.joinToString(separator = " | ") { it[defaultLang] }
        }

        for (candidate in list.candidates) {
            + (boxes(candidate.maxVotes) + candidate.columns.joinToString(separator = " | ") { it.asPlainString(
                defaultLang
            ) } + " [id = ${candidate.id}]" )
            if (candidate.minVotes > 0) {
               + "   [ rules:  min votes = ${candidate.minVotes}] "
            }
       }
   }
}

private fun boxes(n: Int) : String = repeatStr(n, "□ ")


private val defaultLang = I18n.Language.DE

