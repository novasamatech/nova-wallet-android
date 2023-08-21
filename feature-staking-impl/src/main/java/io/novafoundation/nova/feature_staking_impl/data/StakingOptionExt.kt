package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MultiStakingOptionIds
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset

val StakingOption.fullId
    get() = StakingOptionId(chainId = assetWithChain.chain.id, assetWithChain.asset.id, additional.stakingType)

val StakingOption.components: Triple<Chain, Chain.Asset, Chain.Asset.StakingType>
    get() = Triple(assetWithChain.chain, assetWithChain.asset, additional.stakingType)

val StakingOption.chain: Chain
    get() = assetWithChain.chain

val StakingOption.stakingType: Chain.Asset.StakingType
    get() = additional.stakingType

suspend fun ChainRegistry.constructStakingOptions(stakingOptionId: MultiStakingOptionIds): List<StakingOption> {
    val (chain, asset) = chainWithAsset(stakingOptionId.chainId, stakingOptionId.chainAssetId)

    return stakingOptionId.stakingTypes.map { stakingType ->
        createStakingOption(chain, asset, stakingType)
    }
}

suspend fun ChainRegistry.constructStakingOption(stakingOptionId: StakingOptionId): StakingOption {
    val (chain, asset) = chainWithAsset(stakingOptionId.chainId, stakingOptionId.chainAssetId)

    return createStakingOption(chain, asset, stakingOptionId.stakingType)
}
