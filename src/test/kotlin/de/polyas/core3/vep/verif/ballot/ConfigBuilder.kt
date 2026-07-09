@file:Suppress("unused")

package de.polyas.core3.vep.verif.ballot

import de.polyas.core3.vep.verif.ballot.model.AnyContent
import de.polyas.core3.vep.verif.ballot.model.BallotEncodingSpec
import de.polyas.core3.vep.verif.ballot.model.BallotId
import de.polyas.core3.vep.verif.ballot.model.BallotSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateListSpec
import de.polyas.core3.vep.verif.ballot.model.CandidateSpec
import de.polyas.core3.vep.verif.ballot.model.Content
import de.polyas.core3.vep.verif.ballot.model.I18n
import de.polyas.core3.vep.verif.ballot.model.I18nString
import de.polyas.core3.vep.verif.ballot.model.Language

@DslMarker
annotation class ConfBuilderMarker

fun ballotContent(id: String, title: String, defaultLanguage: Language = Language.DE, block: BallotConfigCtx.() -> Unit): BallotSpec {
    val ctx = BallotConfigCtx(id, defaultLanguage)
    ctx.block()

    val maxVotes = ctx.maxVotes
        ?: (ctx.lists.sumOf(CandidateListSpec::maxVotesOnList) + ctx.lists.sumOf(CandidateListSpec::maxVotesForList))

    val ballot = BallotSpec(
        id = BallotId(id),
        title = title.asI18n(),
        contentAbove = ctx._contentAbove,
        contentBelow = ctx._contentBelow,
        lists = ctx.lists,
        showInvalidOption = ctx.showInvalid,
        showAbstainOption = ctx.showAbstainOption,
        maxVotes = maxVotes,
        minVotes = ctx.minVotes,
        maxListsWithChoices = ctx.maxListsWithChoices,
        maxVotesForCandidates =  ctx.maxVotesForCandidates,
        minVotesForCandidates = ctx.minVotesForCandidates,
        maxVotesForLists = ctx.maxVotesForLists,
        minVotesForLists = ctx.minVotesForLists,
        encoding = ctx.encoding
    )
    return ballot
}

@ConfBuilderMarker
class BallotConfigCtx(private val ballotId: String, private val defaultLanguage: Language) {
    private val ids = Ids()
    internal var _contentAbove: AnyContent? = null
    internal var _contentBelow: AnyContent? = null
    internal val lists: MutableList<CandidateListSpec> = mutableListOf()
    var showInvalid = true
    var showAbstainOption = false
    var showSearchBox = false
    var maxVotes: Int? = null
    var minVotes = 0
    var maxListsWithChoices : Int? = null
    var maxVotesForCandidates: Int? = null
    var minVotesForCandidates: Int? = null
    var maxVotesForLists: Int? = null
    var minVotesForLists: Int? = null
    var hideVoteCountForLists: Boolean = false
    var colorSchema: String? = null
    var encoding: BallotEncodingSpec = BallotEncodingSpec.Standard

    fun contentAbove(content: () -> String) {
        _contentAbove = content().asContent()
    }

    fun contentBelow(content: () -> String) {
        _contentBelow = content().asContent()
    }

    /**
     * Configures a list of candidates/choices and adds it to the current ballot.
     */
    fun list(title:String, vararg headers: String, block: ListCtx.() -> Unit) {
        val id = ballotId + ids.next()
        val c = ListCtx(id)
        c.block()
        lists += CandidateListSpec(
            id = id,
            contentAbove = c.contentAbove,
            title = title.asI18n(),
            columnHeaders = headers.map(String::asI18n),
            candidates = c.candidates,
            maxVotesOnList = c.maxVotesOnList,
            minVotesOnList = c.minVotesOnList,
            maxVotesForList = c.maxVotesForList,
            minVotesForList = c.minVotesForList,
            maxVotesTotal = c.maxVotesTotal,
            minVotesTotal = c.minVotesTotal,
            voteCandidateXorList = c.voteCandidateXorList,
            derivedListVotes = c.derivedListVotes,
            derivedCandidateVotes = c.derivedCandidateVotes,
        )
    }

    /**
     * Configures a yes/no choice (as a list with two choices) and adds it to the current ballot.
     */
    fun yesNoChoice(title: String, header: String = "", block: ListCtx.() -> Unit = {}) =
        list(title, header) {
            when (val lang = this@BallotConfigCtx.defaultLanguage) {
                Language.DE -> {
                    choice("Ja")
                    choice("Nein")
                }
                Language.EN -> {
                    choice("Yes")
                    choice("No")
                }
                else -> notHandled(lang)
            }
            minVotesOnList = 1
            maxVotesOnList = 1
            block()
        }
}

@ConfBuilderMarker
class ListCtx(private val listId: String) {
    internal val ids = Ids()
    internal val candidates: MutableList<CandidateSpec> = mutableListOf()

    fun choice(vararg columns: String, block: CandCtx.() -> Unit) {
        val c = CandCtx()
        c.block()
        candidates += CandidateSpec(
            id = listId + "-" + ids.next(),
            columns = columns.map(String::asContent),
            maxVotes = c.maxVotes,
            minVotes = c.minVotes,
            writeInSize = c.writeInSize
        )
    }

    fun choice(vararg columns: String) = choice(*columns) {}

    var maxVotesOnList: Int = 1
    var minVotesOnList: Int = 0
    var maxVotesForList: Int = 0
    var minVotesForList: Int = 0
    var maxVotesTotal: Int? = null
    var minVotesTotal: Int? = null
    var voteCandidateXorList: Boolean = false
    var contentAbove: AnyContent? = null
    var derivedListVotes: CandidateListSpec.DerivedListVotesSpec? = null
    var derivedCandidateVotes: CandidateListSpec.DerivedCandidateVotesSpec? = null
}

@ConfBuilderMarker
class CandCtx {
    internal var columns: List<String> = emptyList()
    var minVotes = 0
    var maxVotes = 1
    var writeInSize: Int? = null
}

private fun notHandled(value: Any) : Nothing {
    throw UnsupportedOperationException("Value $value not handled (probably the pattern matching is not exhaustive)")
}

internal class BallotIds {
    private val letters: CharRange = 'A'..'Z'

    private fun letseq(): Sequence<String> = sequence {
        for (a in letters) {
            yield(a.toString())
        }
        for (s in letseq()) {
            for (a in letters) {
                yield(s + a)
            }
        }
    }

    private val it = letseq().iterator()

    fun next() : String = it.next()
}

internal class Ids {
    private var counter = 0
    fun next() : String = (++counter).toString()
}

private fun String.asI18n() : I18nString = I18n.of(this)
private fun String.asContent() : Content.Text = Content.Text(I18n.of(this))
