package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeChain
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeEarnings
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeEarningsShimmer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeEarningsValueGroup
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsAmount
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsAmountShimmer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsFiat
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRewardsFiatShimmer
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeRightSection
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStakeAmount
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStakesFiat
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStatus
import kotlinx.android.synthetic.main.item_dashboard_has_stake.view.itemDashboardHasStakeStatusShimmer

class StakingDashboardHasStakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ShimmerFrameLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.item_dashboard_has_stake, this)

        background = context.getBlockDrawable().withRippleMask()
        itemDashboardHasStakeRightSection.background = getRoundedCornerDrawable(cornerSizeDp = 10, fillColorRes = R.color.block_background_dark)

        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(context), 8.dp(context), 16.dp(context), 0)
        }

        val shimmer = Shimmer.AlphaHighlightBuilder()
            .setBaseAlpha(0.6f)
            .build()

        setShimmer(shimmer)
    }

    fun setChainUi(chainUi: ChainUi) {
        itemDashboardHasStakeChain.setChain(chainUi)
    }

    fun setRewards(rewardsState: ExtendedLoadingState<AmountModel>) {
        rewardsState.applyToView(itemDashboardHasStakeRewardsFiat, itemDashboardHasStakeRewardsFiatShimmer) { rewards ->
            itemDashboardHasStakeRewardsFiat.text = rewards.fiat
        }

        rewardsState.applyToView(itemDashboardHasStakeRewardsAmount, itemDashboardHasStakeRewardsAmountShimmer) { rewards ->
            itemDashboardHasStakeRewardsAmount.text = rewards.token
        }
    }

    fun setStake(stake: AmountModel) {
        itemDashboardHasStakeStakeAmount.text = stake.token
        itemDashboardHasStakeStakesFiat.setTextOrHide(stake.fiat)
    }

    fun setStatus(status: ExtendedLoadingState<StakeStatusModel>) {
        status.applyToView(itemDashboardHasStakeStatus, itemDashboardHasStakeStatusShimmer) { statusModel ->
            itemDashboardHasStakeStatus.setModel(statusModel)
        }
    }

    fun setEarnings(earningsState: ExtendedLoadingState<String>) {
        earningsState.applyToView(itemDashboardHasStakeEarningsValueGroup, itemDashboardHasStakeEarningsShimmer) { earnings ->
            itemDashboardHasStakeEarnings.text = earnings
        }
    }

    fun setSyncing(isSyncing: Boolean) {
        if (isSyncing) showShimmer(true) else hideShimmer()
    }

    fun unbind() {
        itemDashboardHasStakeChain.unbind()
    }
}
