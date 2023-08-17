package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState.SupportedAssetOption
import kotlinx.coroutines.flow.Flow

typealias StakingOption = SupportedAssetOption<StakingSharedState.OptionAdditionalData>

class StakingSharedState : SelectedAssetOptionSharedState<StakingSharedState.OptionAdditionalData> {

    class OptionAdditionalData(val stakingType: Chain.Asset.StakingType)

    private val _selectedOption = singleReplaySharedFlow<SupportedAssetOption<OptionAdditionalData>>()
    override val selectedOption: Flow<SupportedAssetOption<OptionAdditionalData>> = _selectedOption

    suspend fun setSelectedOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        stakingType: Chain.Asset.StakingType
    ) {
        val selectedOption = createStakingOption(chain, chainAsset, stakingType)

        setSelectedOption(selectedOption)
    }

    suspend fun setSelectedOption(option: StakingOption) {
        _selectedOption.emit(option)
    }
}

fun createStakingOption(chain: Chain, chainAsset: Chain.Asset, stakingType: Chain.Asset.StakingType): StakingOption {
    return StakingOption(
        assetWithChain = ChainWithAsset(chain, chainAsset),
        additional = StakingSharedState.OptionAdditionalData(stakingType)
    )
}
