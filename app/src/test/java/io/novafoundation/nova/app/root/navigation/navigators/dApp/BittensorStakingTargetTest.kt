package io.novafoundation.nova.app.root.navigation.navigators.dApp

import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorStakingTargetTest {

    // The dashboard matches the target against Chain.id, which has no 0x prefix, and the native asset
    private val bittensorChainId = "2f0555cc76fc2840a25a6ea3b9637146806f1f44b090c175ffde2a7e5ab36c03"

    private val nativeAssetId = 0

    @Test
    fun `target is the native asset of Bittensor and carries the flow source of the prompt`() {
        assertEquals(
            StakingDeepLinkTarget(bittensorChainId, nativeAssetId, StakingFlowSource.DAPP_NOTICE),
            bittensorStakingTarget(StakingFlowSource.DAPP_NOTICE)
        )
        assertEquals(
            StakingDeepLinkTarget(bittensorChainId, nativeAssetId, StakingFlowSource.DAPP_SEARCH_BANNER),
            bittensorStakingTarget(StakingFlowSource.DAPP_SEARCH_BANNER)
        )
    }
}
