package io.novafoundation.nova.common.utils

fun <T> List<T>.isSubsetOf(list: List<T>): Boolean {
    return list.containsAll(this)
}

fun <T> Collection<T>.isAllEquals(value: (T) -> Any): Boolean {
    if (isEmpty()) return false

    val first = value(first())
    return all { value(it) == first }
}

inline fun <T> List<T>.binarySearchFloor(fromIndex: Int = 0, toIndex: Int = size, comparison: (T) -> Int): Int {
    rangeCheck(fromIndex, toIndex)

    if (this.isEmpty()) return -1

    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = (low + high).ushr(1)
        val midVal = get(mid)
        val cmp = comparison(midVal)

        if (cmp < 0) {
            low = mid + 1
        } else if (cmp > 0) {
            high = mid - 1
        } else {
            return mid // key found
        }
    }

    // key not found. Takes floor key
    return if (low <= 0) {
        0
    } else if (low >= size) {
        size - 1
    } else {
        low - 1
    }
}

fun <T> List<T>.rangeCheck(fromIndex: Int, toIndex: Int) {
    when {
        fromIndex > toIndex -> throw IllegalArgumentException("fromIndex ($fromIndex) is greater than toIndex ($toIndex).")
        fromIndex < 0 -> throw IndexOutOfBoundsException("fromIndex ($fromIndex) is less than zero.")
        toIndex > size -> throw IndexOutOfBoundsException("toIndex ($toIndex) is greater than size ($size).")
    }
}

fun <T> List<T>.findByStep(fromPosition: Int, step: Int, predicate: (T) -> Boolean): T? {
    return findIndexByStep(fromPosition, step, predicate)?.let { get(it) }
}

fun <T> List<T>.findIndexByStep(fromPosition: Int, step: Int, predicate: (T) -> Boolean): Int? {
    if (fromPosition < 0) return null
    if (fromPosition >= size) return null
    if (step == 0) return null

    var currentPosition = fromPosition
    while (currentPosition in 0 until size) {
        if (predicate(get(currentPosition))) {
            return currentPosition
        }

        currentPosition += step
    }

    return null
}

fun <T> Sequence<T>.subList(fromIndex: Int, toIndex: Int): List<T> {
    return toList().subList(fromIndex, toIndex)
}

