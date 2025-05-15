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

