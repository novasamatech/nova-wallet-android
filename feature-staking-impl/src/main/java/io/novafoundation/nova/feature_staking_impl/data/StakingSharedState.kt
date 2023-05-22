package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState.SupportedAssetOption

class StakingSharedState : SelectedAssetOptionSharedState<StakingSharedState.OptionAdditionalData> {

    class OptionAdditionalData(val stakingType: Chain.Asset.StakingType)

    override val selectedOption = singleReplaySharedFlow<SupportedAssetOption<OptionAdditionalData>>()
}
