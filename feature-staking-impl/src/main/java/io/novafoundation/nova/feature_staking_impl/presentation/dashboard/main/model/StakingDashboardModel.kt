package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.SyncingData
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class StakingDashboardModel(
    val hasStakeItems: List<HasStakeItem>,
    val noStakeItems: List<NoStakeItem>,
) {

    data class HasStakeItem(
        val assetLabel: SyncingData<String>,
        override val assetId: FullChainAssetId,
        override val stakingTypeBadge: StakingTypeModel?,
        override val assetIcon: SyncingData<Icon>,
        val rewards: ExtendedLoadingState<SyncingData<MaskableModel<AmountModel>>>,
        val stake: SyncingData<MaskableModel<AmountModel>>,
        val status: ExtendedLoadingState<SyncingData<StakeStatusModel>>,
        val earnings: ExtendedLoadingState<SyncingData<String>>,
    ) : BaseItem

    data class NoStakeItem(
        override val stakingTypeBadge: StakingTypeModel?,
        override val assetId: FullChainAssetId,
        override val assetIcon: SyncingData<Icon>,
        val tokenName: SyncingData<String>,
        val availableBalance: CharSequence?,
        val earnings: ExtendedLoadingState<SyncingData<String>>,
    ) : BaseItem

    interface BaseItem {
        val stakingTypeBadge: StakingTypeModel?
        val assetId: FullChainAssetId
        val assetIcon: SyncingData<Icon>
    }

    data class StakingTypeModel(@DrawableRes val icon: Int, val text: String)
}
