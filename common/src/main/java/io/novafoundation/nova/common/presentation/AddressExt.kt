package io.novafoundation.nova.common.presentation

fun String.toShortAddressFormat(): String {
    if (length > 13) {
        return take(5) + "..." + takeLast(5)
    }

    return this
}
