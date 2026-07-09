/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.ballot.validation

/**
 * An object representing the result of validation of a value of type T.
 */
sealed class Validated<T> {
    abstract val value: T

    data class Valid<T>(override val value: T) : Validated<T>()

    data class Malformed<T>(override val value: T) : Validated<T>()

    data class Invalid<T>(override val value: T) : Validated<T>()

    data class IntentionallyInvalid<T>(override val value: T) : Validated<T>()

    data class Abstained<T>(override val value: T) : Validated<T>()
}

val<T> T.asValid get() = Validated.Valid(this)

val<T> T.asInvalid get() = Validated.Invalid(this)

val<T> T.asMalformed get() = Validated.Malformed(this)

val<T> T.asIntentionallyInvalid get() = Validated.IntentionallyInvalid(this)

val<T> T.asAbstained get() = Validated.Abstained(this)

/**
 * Returns this validated object, if it is not valid; otherwise executes the given [validationBlock]
 * and returns its result.
 */
fun<T> Validated<T>.furtherValidate(validationBlock: (T) -> Validated<T>): Validated<T> =
    when (this) {
        is Validated.Valid -> validationBlock(this.value)
        else -> this
    }
