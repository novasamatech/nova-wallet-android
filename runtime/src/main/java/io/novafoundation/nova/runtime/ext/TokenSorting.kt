package io.novafoundation.nova.runtime.ext

import io.novafoundation.nova.common.utils.TokenSymbol

val TokenSymbol.mainTokensFirstAscendingOrder
    get() = when (this.value) {
        "DOT" -> 0
        "KSM" -> 1
        "USDT" -> 2
        "USDC" -> 3
        else -> 4
    }

val TokenSymbol.alphabeticalOrder
    get() = value

fun <K> TokenSymbol.Companion.defaultComparatorFrom(extractor: (K) -> TokenSymbol): Comparator<K> = Comparator.comparing(extractor, defaultComparator())

fun TokenSymbol.Companion.defaultComparator(): Comparator<TokenSymbol> = compareBy<TokenSymbol> { it.mainTokensFirstAscendingOrder }
    .thenBy { it.alphabeticalOrder }
