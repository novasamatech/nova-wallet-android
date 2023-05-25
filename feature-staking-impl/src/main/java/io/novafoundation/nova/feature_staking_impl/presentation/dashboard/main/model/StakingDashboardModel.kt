package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

class StakingDashboardModel(
    val hasStakeItems: List<HasStakeItem>,
    val noStakeItems: List<NoStakeItem>,
) {

    data class HasStakeItem(
        override val chainUi: ChainUi,
        override val assetId: ChainAssetId,
        val rewards: ExtendedLoadingState<AmountModel>,
        val stake: AmountModel,
        val status: ExtendedLoadingState<StakeStatusModel>,
        val earnings: ExtendedLoadingState<String>,
        override val syncingStage: SyncingStage
    ) : BaseItem

    data class NoStakeItem(
        override val chainUi: ChainUi,
        override val assetId: ChainAssetId,
        val earnings: ExtendedLoadingState<String>,
        val availableBalance: String?,
        override val syncingStage: SyncingStage
    ) : BaseItem

    interface BaseItem {
        val chainUi: ChainUi
        val assetId: ChainAssetId
        val syncingStage: SyncingStage
    }
}
