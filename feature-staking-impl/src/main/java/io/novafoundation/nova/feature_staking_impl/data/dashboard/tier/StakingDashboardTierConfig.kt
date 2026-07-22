package io.novafoundation.nova.feature_staking_impl.data.dashboard.tier

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.KnownChainIds

object StakingDashboardTierConfig {
    val demotedChainIds: Set<ChainId> = setOf(
        KnownChainIds.MANTA_ATLANTIC,
        KnownChainIds.ALEPH_ZERO,
        KnownChainIds.VARA,
        KnownChainIds.ZEITGEIST,
        KnownChainIds.MOONRIVER,
        KnownChainIds.POLKADEX,
        KnownChainIds.TERNOA,
    )
}
