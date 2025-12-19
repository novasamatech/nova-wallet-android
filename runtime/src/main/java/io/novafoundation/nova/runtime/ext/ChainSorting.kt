package io.novafoundation.nova.runtime.ext

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

val Chain.mainChainsFirstAscendingOrder
    get() = this.displayPriority ?: Int.MAX_VALUE

val Chain.testnetsLastAscendingOrder
    get() = if (isTestNet) {
        1
    } else {
        0
    }

val Chain.alphabeticalOrder
    get() = name

fun <K> Chain.Companion.defaultComparatorFrom(extractor: (K) -> Chain): Comparator<K> = Comparator.comparing(extractor, defaultComparator())

fun Chain.Companion.defaultComparator(): Comparator<Chain> = compareBy<Chain> { it.mainChainsFirstAscendingOrder }
    .thenBy { it.testnetsLastAscendingOrder }
    .thenBy { it.alphabeticalOrder }
