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
        /** Hide both fiat-under-stake lines (rewards fiat + stake fiat).
         *  True only for subnet-alpha rows, which have no priceId. iOS does
         *  the same — alpha never has a CoinGecko feed. Root TAO keeps fiat. */
        val hideFiat: Boolean = false,
        /** Hide the "X.XX% per year" group (label + value + suffix). True
         *  for all Subtensor rows — Bittensor has no off-chain rewards
         *  indexer, so the estimated APY is meaningless on both root and
         *  subnet. Mirrors iOS dashboard mapper. */
        val hideEarnings: Boolean = false,
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
