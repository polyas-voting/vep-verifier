package de.polyas.core3.vep.verif.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

typealias AnyContent = Content<*>

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "contentType")
@JsonSubTypes(
    JsonSubTypes.Type(value = Content.Text::class, name = "TEXT"),
    JsonSubTypes.Type(value = Content.RichText::class, name = "RICH_TEXT")
)
sealed class Content<T>(val contentType: Type) {
    abstract val value: I18n<T>

    /**
     * Internationalized plain value. No formatting except that an empty line indicates a new paragraph.
     */
    data class Text(override val value: I18nString) : Content<String>(Type.TEXT)

    /**
     * Internationalized rich text value, based on the Slate format.
     */
    data class RichText(override val value: I18n<RichTextDocument>) : Content<RichTextDocument>(Type.RICH_TEXT)

    enum class Type { TEXT, RICH_TEXT }
}