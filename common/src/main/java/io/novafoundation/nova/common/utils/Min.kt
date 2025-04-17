package io.novafoundation.nova.common.utils

/**
 * Interface that can be used to get access to `min` and `max` functions on collections
 * when `Comparable` is not implementable for a given type.
 *
 * In particular, `Comparable` implies presence of the total ordering for a type.
 * However, even types with partial ordering, do, in principle, support `Min` and `Max` operations
 */
interface Min<T : Min<T>> {

    /**
     * Find minimum between self and [other]
     * Should be commutative, i.e. `a.min(b) = b.min(a)`
     */
    fun min(other: T): T
}

fun <T : Min<T>> min(first: T, vararg rest: T): T {
    return rest.fold(first) { acc, item ->
        acc.min(item)
    }
}
