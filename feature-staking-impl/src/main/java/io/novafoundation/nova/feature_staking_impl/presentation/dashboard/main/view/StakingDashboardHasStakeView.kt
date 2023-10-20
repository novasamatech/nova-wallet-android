package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setShimmerShown
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.unsafeLazy
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.StakingTypeModel
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeChain
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeChainContainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeEarnings
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeEarningsContainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeEarningsShimmer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeEarningsSuffix
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsAmount
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsAmountShimmer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsAmountСontainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsFiat
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsFiatContainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsFiatShimmer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsLabel
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsLabelShimmer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsLabelСontainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRightSection
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStakeAmount
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStakeAmountContainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStakesFiat
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStakesFiatContainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStakingType
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStatus
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStatusContainer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStatusShimmer

class StakingDashboardHasStakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val rewardsLabelGroup by unsafeLazy {
        ShimmerableGroup(
            container = itemDashboardHasStakeRewardsLabelСontainer,
            shimmerShape = itemDashboardHasStakeRewardsLabelShimmer,
            content = itemDashboardHasStakeRewardsLabel
        )
    }

    private val rewardsAmountGroup by unsafeLazy {
        ShimmerableGroup(
            container = itemDashboardHasStakeRewardsAmountСontainer,
            shimmerShape = itemDashboardHasStakeRewardsAmountShimmer,
            content = itemDashboardHasStakeRewardsAmount
        )
    }

    private val rewardsFiatGroup by unsafeLazy {
        ShimmerableGroup(
            container = itemDashboardHasStakeRewardsFiatContainer,
            shimmerShape = itemDashboardHasStakeRewardsFiatShimmer,
            content = itemDashboardHasStakeRewardsFiat
        )
    }

    private val stakeStatusGroup by unsafeLazy {
        ShimmerableGroup(
            container = itemDashboardHasStakeStatusContainer,
            shimmerShape = itemDashboardHasStakeStatusShimmer,
            content = itemDashboardHasStakeStatus
        )
    }

    private val earningsGroup by unsafeLazy {
        ShimmerableGroup(
            container = itemDashboardHasStakeEarningsContainer,
            shimmerShape = itemDashboardHasStakeEarningsShimmer,
            content = itemDashboardHasStakeEarnings
        )
    }

    init {
        View.inflate(context, R.layout.item_dashboard_has_stake, this)

        background = context.getBlockDrawable().withRippleMask()
        itemDashboardHasStakeRightSection.background = getRoundedCornerDrawable(cornerSizeDp = 10, fillColorRes = R.color.block_background_dark)

        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(context), 8.dp(context), 16.dp(context), 0)
        }
    }

    fun setChainUi(chainUi: SyncingData<ChainUi>) {
        itemDashboardHasStakeChain.setChain(chainUi.data)
        itemDashboardHasStakeChainContainer.setShimmerShown(chainUi.isSyncing)
    }

    fun setRewards(rewardsState: ExtendedLoadingState<SyncingData<AmountModel>>) {
        rewardsLabelGroup.applyState(rewardsState)
        rewardsAmountGroup.applyState(rewardsState) { text = it.token }
        rewardsFiatGroup.applyState(rewardsState) { text = it.fiat }
    }

    fun setStake(stake: SyncingData<AmountModel>) {
        itemDashboardHasStakeStakeAmount.text = stake.data.token
        itemDashboardHasStakeStakeAmountContainer.setShimmerShown(stake.isSyncing)

        itemDashboardHasStakeStakesFiat.setTextOrHide(stake.data.fiat)
        itemDashboardHasStakeStakesFiatContainer.setShimmerShown(stake.isSyncing)
    }

    fun setStatus(status: ExtendedLoadingState<SyncingData<StakeStatusModel>>) {
        stakeStatusGroup.applyState(status) { setModel(it) }
    }

    fun setEarnings(earningsState: ExtendedLoadingState<SyncingData<String>>) {
        earningsGroup.applyState(earningsState) { text = it }

        itemDashboardHasStakeEarningsSuffix.setVisible(earningsState.isLoaded())
    }

    fun setStakingTypeBadge(model: StakingTypeModel?) {
        itemDashboardHasStakeStakingType.setModelOrHide(model)
    }

    fun unbind() {
        itemDashboardHasStakeChain.unbind()
    }
}
