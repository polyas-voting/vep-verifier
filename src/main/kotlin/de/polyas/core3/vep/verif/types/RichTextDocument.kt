/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Rich text data model, compatible with the Slate format
 */
data class RichTextDocument(
    val data: Map<String, String> = emptyMap(),
    val nodes: List<Node>,
    val `object`: ObjectType = ObjectType.document
) {

    @Suppress("EnumEntryName")
    enum class ObjectType { document, block, inline, text }

    /**
     * A node of type [Block], [Inline], or [Text].
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "object")
    @JsonSubTypes(
        JsonSubTypes.Type(value = Node.Block::class, name = "block"),
        JsonSubTypes.Type(value = Node.Inline::class, name = "inline"),
        JsonSubTypes.Type(value = Node.Text::class, name = "text")
    )
    sealed class Node(val `object`: ObjectType) {

        data class Text(
            val text: String,
            val marks: Any // no details modeling
        ) : Node(ObjectType.text)

        data class Block(
            val nodes: List<Node>,
            val type: String,
            val data: Any // no details modeling
        ) : Node(ObjectType.block)

        data class Inline(
            val nodes: List<Node>,
            val type: String,
            val data: Any // no details modeling
        ) : Node(ObjectType.inline)
    }
}
