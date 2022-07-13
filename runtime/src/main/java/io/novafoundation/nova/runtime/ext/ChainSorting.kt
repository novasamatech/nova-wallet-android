package io.novafoundation.nova.runtime.ext

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

val Chain.relaychainsFirstAscendingOrder
    get() = when (genesisHash) {
        Chain.Geneses.POLKADOT -> 0
        Chain.Geneses.KUSAMA -> 1
        else -> 2
    }

val Chain.testnetsLastAscendingOrder
    get() = if (isTestNet) {
        1
    } else {
        0
    }

val Chain.alpabeticalOrder
    get() = name

fun <K> Chain.Companion.defaultComparatorFrom(extractor: (K) -> Chain): Comparator<K> = Comparator.comparing(extractor, defaultComparator())

fun Chain.Companion.defaultComparator(): Comparator<Chain> = compareBy<Chain> { it.relaychainsFirstAscendingOrder }
    .thenBy { it.testnetsLastAscendingOrder }
    .thenBy { it.alpabeticalOrder }
