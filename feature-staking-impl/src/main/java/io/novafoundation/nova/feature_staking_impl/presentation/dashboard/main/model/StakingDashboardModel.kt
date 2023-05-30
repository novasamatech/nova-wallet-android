package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
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
        val rewards: ExtendedLoadingState<AmountModel>,
        val stake: AmountModel,
        val status: ExtendedLoadingState<StakeStatusModel>,
        val earnings: ExtendedLoadingState<String>,
        val syncingStage: SyncingStage
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
