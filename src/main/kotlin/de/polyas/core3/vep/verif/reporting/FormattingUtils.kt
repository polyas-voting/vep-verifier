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

import de.polyas.core3.vep.verif.ballot.model.AnyContent
import de.polyas.core3.vep.verif.ballot.model.Content
import de.polyas.core3.vep.verif.ballot.model.Language
import de.polyas.core3.vep.verif.ballot.model.RichTextDocument

fun repeatStr(n: Int, str: String) : String =
    (1..n).joinToString(separator = "") { str }

fun AnyContent.asPlainString(language: Language) : String = when (this) {
    is Content.Text -> value[language]
    is Content.RichText -> value[language].asPlainString()
}

private fun RichTextDocument.asPlainString() : String =
    when (val t = this.text) {
        null -> nodes?.joinToString { it.asPlainString() } ?: ""
        else -> t
    }