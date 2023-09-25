package io.novafoundation.nova.common.utils

interface SemiUnboundedRange<T : Comparable<T>> {

    val start: T

    val endInclusive: T?
}

infix operator fun <T : Comparable<T>> T.rangeTo(another: T?): SemiUnboundedRange<T> {
    return ComparableSemiUnboundedRange(this, another)
}

inline fun <T : Comparable<T>, R : Comparable<R>> SemiUnboundedRange<T>.map(mapper: (T) -> R): SemiUnboundedRange<R> {
    return mapper(start)..endInclusive?.let(mapper)
}

class ComparableSemiUnboundedRange<T : Comparable<T>>(override val start: T, override val endInclusive: T?) : SemiUnboundedRange<T>
