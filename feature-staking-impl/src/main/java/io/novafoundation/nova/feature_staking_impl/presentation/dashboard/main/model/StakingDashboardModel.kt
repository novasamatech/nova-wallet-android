package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.SyncingData
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

class StakingDashboardModel(
    val hasStakeItems: List<HasStakeItem>,
    val noStakeItems: List<NoStakeItem>,
) {

    data class HasStakeItem(
        override val chainUi: SyncingData<ChainUi>,
        override val assetId: ChainAssetId,
        override val stakingTypeBadge: StakingTypeModel?,
        val rewards: ExtendedLoadingState<SyncingData<MaskableModel<AmountModel>>>,
        val stake: SyncingData<MaskableModel<AmountModel>>,
        val status: ExtendedLoadingState<SyncingData<StakeStatusModel>>,
        val earnings: ExtendedLoadingState<SyncingData<String>>,
    ) : BaseItem

    data class NoStakeItem(
        override val chainUi: SyncingData<ChainUi>,
        val availableBalance: CharSequence?,
        override val stakingTypeBadge: StakingTypeModel?,
        override val assetId: ChainAssetId,
        val earnings: ExtendedLoadingState<SyncingData<String>>,
    ) : BaseItem

    interface BaseItem {
        val stakingTypeBadge: StakingTypeModel?
        val chainUi: SyncingData<ChainUi>
        val assetId: ChainAssetId
    }

    data class StakingTypeModel(@DrawableRes val icon: Int, val text: String)
}
