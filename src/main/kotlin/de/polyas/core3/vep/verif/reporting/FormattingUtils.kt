package de.polyas.core3.vep.verif.reporting

import de.polyas.core3.vep.verif.types.AnyContent
import de.polyas.core3.vep.verif.types.Content
import de.polyas.core3.vep.verif.types.I18n
import de.polyas.core3.vep.verif.types.RichTextDocument

fun repeatStr(n: Int, str: String) : String =
    (1..n).joinToString(separator = "") { str }

fun AnyContent.asPlainString(language: I18n.Language) : String = when (this) {
    is Content.Text -> value[language]
    is Content.RichText -> value[language].asPlainString()
}

private fun RichTextDocument.asPlainString() : String =
    nodes.joinToString { it.asPlainString() }

private fun RichTextDocument.Node.asPlainString() : String = when (this) {
    is RichTextDocument.Node.Text
        ->  text
    is RichTextDocument.Node.Block
        -> nodes.joinToString { it.asPlainString() }
    is RichTextDocument.Node.Inline
        -> nodes.joinToString { it.asPlainString() }
}


