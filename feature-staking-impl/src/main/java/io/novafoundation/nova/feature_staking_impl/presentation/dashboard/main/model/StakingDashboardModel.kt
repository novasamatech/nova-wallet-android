package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
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
        val rewards: ExtendedLoadingState<SyncingData<AmountModel>>,
        val stake: SyncingData<AmountModel>,
        val status: ExtendedLoadingState<SyncingData<StakeStatusModel>>,
        val earnings: ExtendedLoadingState<SyncingData<String>>,
    ) : BaseItem

    data class NoStakeItem(
        override val chainUi: SyncingData<ChainUi>,
        override val assetId: ChainAssetId,
        val earnings: ExtendedLoadingState<SyncingData<String>>,
        val availableBalance: String?,
    ) : BaseItem

    interface BaseItem {
        val chainUi: SyncingData<ChainUi>
        val assetId: ChainAssetId
    }
}
