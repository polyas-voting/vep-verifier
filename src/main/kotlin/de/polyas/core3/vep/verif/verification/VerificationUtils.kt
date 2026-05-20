/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.verification

fun <T> Sequence<T>.containsDuplicates(): Boolean {
    val elements = mutableSetOf<T>()
    for (e in this) {
        if (e in elements) return true
        elements += e
    }
    return false
}

fun <T> Sequence<T>.containsNoDuplicates(): Boolean = !containsDuplicates()

fun<T> equalSequences(a: Sequence<T>, b: Sequence<T>) : Boolean {
    val itA = a.iterator()
    val itB = b.iterator()
    while (itA.hasNext() && itB.hasNext()) {
        if (itA.next() != itB.next()) return false
    }
    return !itA.hasNext() && !itB.hasNext()
}

/**
 * Applies the given block to each pair of corresponding elements of the given two sequences.
 * Makes sure that the sequences are of the same length (throws otherwise).
 */
fun<A,B> applyToCorresponding(a: Sequence<A>, b: Sequence<B>, block: (A,B) -> Unit) {
    applyToCorresponding(a.asIterable(), b.asIterable(), block)
}

/**
 * Applies the given block to each pair of corresponding elements of the given two iterables.
 * Makes sure that the sequences are of the same length (throws otherwise).
 */
fun<A,B> applyToCorresponding(a: Iterable<A>, b: Iterable<B>, block: (A,B) -> Unit) {
    val itA = a.iterator()
    val itB = b.iterator()
    while (itA.hasNext() && itB.hasNext()) {
        block(itA.next(), itB.next())
    }
    check(!itA.hasNext() && !itB.hasNext()) {"the number of input and output elements is not the same"}
}

