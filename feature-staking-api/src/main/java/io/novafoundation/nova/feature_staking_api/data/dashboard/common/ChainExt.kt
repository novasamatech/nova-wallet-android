package io.novafoundation.nova.feature_staking_api.data.dashboard.common

import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChains
import io.novafoundation.nova.runtime.multiNetwork.findChainsById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun ChainRegistry.stakingChainsFlow(): Flow<List<Chain>> {
    return currentChains.map { chains ->
        chains.filter { it.utilityAsset.supportedStakingOptions().isNotEmpty() }
    }
}

suspend fun ChainRegistry.stakingChains(): List<Chain> {
    return findChains { it.utilityAsset.supportedStakingOptions().isNotEmpty() }
}

suspend fun ChainRegistry.stakingChainsById(): ChainsById {
    return findChainsById { it.utilityAsset.supportedStakingOptions().isNotEmpty() }
}
