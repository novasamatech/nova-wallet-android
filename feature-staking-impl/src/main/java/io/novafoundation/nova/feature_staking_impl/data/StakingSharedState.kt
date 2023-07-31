package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState.SupportedAssetOption
import kotlinx.coroutines.flow.Flow

typealias StakingOption = SupportedAssetOption<StakingSharedState.OptionAdditionalData>

val StakingOption.fullId
    get() = StakingOptionId(chainId = assetWithChain.chain.id, assetWithChain.asset.id, additional.stakingType)

val StakingOption.components: Triple<Chain, Chain.Asset, Chain.Asset.StakingType>
    get() = Triple(assetWithChain.chain, assetWithChain.asset, additional.stakingType)

class StakingSharedState : SelectedAssetOptionSharedState<StakingSharedState.OptionAdditionalData> {

    class OptionAdditionalData(val stakingType: Chain.Asset.StakingType)

    private val _selectedOption = singleReplaySharedFlow<SupportedAssetOption<OptionAdditionalData>>()
    override val selectedOption: Flow<SupportedAssetOption<OptionAdditionalData>> = _selectedOption

    suspend fun setSelectedOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        stakingType: Chain.Asset.StakingType
    ) {
        val selectedOption = SupportedAssetOption(
            assetWithChain = ChainWithAsset(chain, chainAsset),
            additional = OptionAdditionalData(stakingType)
        )

        setSelectedOption(selectedOption)
    }

    suspend fun setSelectedOption(option: StakingOption) {
        _selectedOption.emit(option)
    }
}
