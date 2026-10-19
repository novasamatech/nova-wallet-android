package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal fun mapNewChainFeatures(value: Any?): Set<Chain.NewFeature> = (value as? List<*>)
    .orEmpty()
    .mapNotNull { if (it == "staking") Chain.NewFeature.STAKING else null }
    .toSet()
