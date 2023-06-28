package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState.SupportedAssetOption
import kotlinx.coroutines.flow.Flow

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

        _selectedOption.emit(selectedOption)
    }
}
