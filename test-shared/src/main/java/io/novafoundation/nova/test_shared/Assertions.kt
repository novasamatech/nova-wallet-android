package io.novafoundation.nova.test_shared

import io.novasama.substrate_sdk_android.extensions.toHexString
import org.junit.Assert.assertEquals

fun <T> assertListEquals(expected: List<T>, actual: List<T>, compare: (T, T) -> Boolean = { a, b -> a == b }) {
    if (expected.size != actual.size) {
        throw AssertionError("Lists are not equal. Expected: $expected, actual: $actual")
    }
    for (i in expected.indices) {
        if (!compare(expected[i], actual[i])) {
            throw AssertionError("Lists are not equal. Expected: $expected, actual: $actual")
        }
    }
}

fun assertHexEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.toHexString(), actual.toHexString())
}

fun <T> assertSetEquals(expected: Set<T>, actual: Set<T>) {
    if (expected != actual) {
        throw AssertionError("Sets are not equal. Expected: $expected, actual: $actual")
    }
}

fun <K, V> assertMapEquals(expected: Map<K, V>, actual: Map<K, V>) {
    if (expected != actual) {
        throw AssertionError("Maps are not equal. Expected: $expected, actual: $actual")
    }
}

fun <V> assertAllItemsEquals(items: List<V>) {
    items.forEach {
        if (it != items[0]) {
            throw AssertionError("Items in list are not equal:\n$it\n${items[0]}")
        }
    }
}
