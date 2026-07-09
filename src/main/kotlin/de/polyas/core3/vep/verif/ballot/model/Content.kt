/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

typealias AnyContent = Content<*>

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "contentType")
@JsonSubTypes(
    JsonSubTypes.Type(value = Content.Text::class, name = "TEXT"),
    JsonSubTypes.Type(value = Content.RichText::class, name = "RICH_TEXT")
)
/** Content with a value of some type `T` with internationalization. */
sealed class Content<T> {
    abstract val value: I18n<T>

    data class Text(override val value: I18nString) : Content<String>()
    data class RichText(override val value: I18n<RichTextDocument>) : Content<RichTextDocument>()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RichTextDocument( // not full modeling
    val text: String? = null,
    val nodes: List<RichTextDocument>? = null,
    // val data: Map<String,String>? = null,
    // val type: String? = null,
)
