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


