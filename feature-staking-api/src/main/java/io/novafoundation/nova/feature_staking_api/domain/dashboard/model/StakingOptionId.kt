package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class StakingOptionId(val chainId: ChainId, val chainAssetId: ChainAssetId, val stakingType: Chain.Asset.StakingType)

data class MultiStakingOptionIds(val chainId: ChainId, val chainAssetId: ChainAssetId, val stakingTypes: List<Chain.Asset.StakingType>)
