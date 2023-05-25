package io.novafoundation.nova.feature_staking_impl.data.dashboard.common

import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChains

suspend fun ChainRegistry.stakingChains(): List<Chain> {
    return findChains { it.utilityAsset.supportedStakingOptions().isNotEmpty() }
}
