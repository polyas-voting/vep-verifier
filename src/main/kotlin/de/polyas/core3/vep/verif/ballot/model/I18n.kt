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

import com.fasterxml.jackson.annotation.JsonValue

data class Language(
    @get:JsonValue val languageCode: String
) {
    override fun toString(): String = languageCode

    companion object {
        val DE = Language("DE")
        val EN = Language("DE")
    }
}

/** A value of type `T` with internationalization (possibly different values for different languages) */
data class I18n<T>(
    /** The default value (used when there is no values specified for the requested language) */
    val default: T,

    /** A mapping from languages to values of type `T` */
    val value: Map<Language, T>
) {
    operator fun get(language: Language): T =
        value[language] ?: default

    companion object {
        fun <T> of(default:T, vararg ps: Pair<Language,T>): I18n<T> = I18n(default, mapOf(*ps))
    }
}

operator fun <T> I18n<T>.get(language: Language): T =
    value[language] ?: default

typealias I18nString = I18n<String>
