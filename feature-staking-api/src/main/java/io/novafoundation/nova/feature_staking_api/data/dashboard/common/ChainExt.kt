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

suspend fun ChainRegistry.stakingChains(): List<Chain> {
    return findChains {  it.enabled && it.supportedStakingOptions() }
}

suspend fun ChainRegistry.stakingChainsById(): ChainsById {
    return findChainsById { it.enabled && it.supportedStakingOptions() }
}

fun Chain.supportedStakingOptions(): Boolean {
    return utilityAsset.supportedStakingOptions().isNotEmpty()
}
