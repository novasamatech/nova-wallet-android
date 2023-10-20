package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapStakingTypeToSubQueryId(stakingType: Chain.Asset.StakingType): String? {
    return when (stakingType) {
        Chain.Asset.StakingType.UNSUPPORTED -> null
        Chain.Asset.StakingType.RELAYCHAIN -> "relaychain"
        Chain.Asset.StakingType.PARACHAIN -> "parachain"
        Chain.Asset.StakingType.RELAYCHAIN_AURA -> "aura-relaychain"
        Chain.Asset.StakingType.TURING -> "turing"
        Chain.Asset.StakingType.ALEPH_ZERO -> "aleph-zero"
        Chain.Asset.StakingType.NOMINATION_POOLS -> "nomination-pool"
    }
}

fun mapSubQueryIdToStakingType(subQueryStakingTypeId: String?): Chain.Asset.StakingType {
    return when (subQueryStakingTypeId) {
        null -> Chain.Asset.StakingType.UNSUPPORTED
        "relaychain" -> Chain.Asset.StakingType.RELAYCHAIN
        "parachain" -> Chain.Asset.StakingType.PARACHAIN
        "aura-relaychain" -> Chain.Asset.StakingType.RELAYCHAIN_AURA
        "turing" -> Chain.Asset.StakingType.TURING
        "aleph-zero" -> Chain.Asset.StakingType.ALEPH_ZERO
        "nomination-pool" -> Chain.Asset.StakingType.NOMINATION_POOLS
        else -> Chain.Asset.StakingType.UNSUPPORTED
    }
}
