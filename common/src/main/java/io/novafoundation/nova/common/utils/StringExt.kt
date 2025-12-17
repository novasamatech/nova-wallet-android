package io.novafoundation.nova.common.utils

fun String.removeSpacing(): String {
    return replace(" ", "")
}

fun CharSequence.ellipsizeMiddle(shownSymbols: Int): CharSequence {
    if (length < shownSymbols * 2) return this

    return StringBuilder(take(shownSymbols))
        .append("...")
        .append(takeLast(shownSymbols))
}
