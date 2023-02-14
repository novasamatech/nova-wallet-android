package io.novafoundation.nova.common.utils

@Suppress("UNCHECKED_CAST")
inline fun <K, reified R> Map<K, *>.filterValuesIsInstance(): Map<K, R> {
    return filterValues { value -> value is R } as Map<K, R>
}
