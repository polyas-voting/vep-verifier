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

/**
 * An internationalized value of a generic type T
 *
 * @param default  The default value to be used when there is no value defined for the requested language
 * @param value    Mapping from languages to values of type T
 */
data class I18n<T>(val default: T, val value: Map<Language, T>) {

    companion object {
        fun<T> of(default:T, vararg ps: Pair<Language,T>) : I18n<T> = I18n(default, mapOf(*ps))
    }

    operator fun get(language: Language): T =
        value[language] ?: default

    enum class Language {
        DE, EN, FR, FI, IT, PL, NL, CZ, ES, NO, DK, ROU, SVK, SE, RU, HU, AR
    }
}


typealias I18nString = I18n<String>