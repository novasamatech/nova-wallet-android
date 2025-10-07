package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setShimmerShown
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.unsafeLazy
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemDashboardHasStakeBinding
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.StakingTypeModel
import io.novafoundation.nova.feature_staking_impl.presentation.view.StakeStatusModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableFiat
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableToken

class StakingDashboardHasStakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ItemDashboardHasStakeBinding.inflate(inflater(), this)

    private val rewardsLabelGroup by unsafeLazy {
        ShimmerableGroup(
            container = binder.itemDashboardHasStakeRewardsLabelContainer,
            shimmerShape = binder.itemDashboardHasStakeRewardsLabelShimmer,
            content = binder.itemDashboardHasStakeRewardsLabel
        )
    }

    private val rewardsAmountGroup by unsafeLazy {
        ShimmerableGroup(
            container = binder.itemDashboardHasStakeRewardsAmountContainer,
            shimmerShape = binder.itemDashboardHasStakeRewardsAmountShimmer,
            content = binder.itemDashboardHasStakeRewardsAmount
        )
    }

    private val rewardsFiatGroup by unsafeLazy {
        ShimmerableGroup(
            container = binder.itemDashboardHasStakeRewardsFiatContainer,
            shimmerShape = binder.itemDashboardHasStakeRewardsFiatShimmer,
            content = binder.itemDashboardHasStakeRewardsFiat
        )
    }

    private val stakeStatusGroup by unsafeLazy {
        ShimmerableGroup(
            container = binder.itemDashboardHasStakeStatusContainer,
            shimmerShape = binder.itemDashboardHasStakeStatusShimmer,
            content = binder.itemDashboardHasStakeStatus
        )
    }

    private val earningsGroup by unsafeLazy {
        ShimmerableGroup(
            container = binder.itemDashboardHasStakeEarningsContainer,
            shimmerShape = binder.itemDashboardHasStakeEarningsShimmer,
            content = binder.itemDashboardHasStakeEarnings
        )
    }

    init {
        background = context.getBlockDrawable().withRippleMask()
        binder.itemDashboardHasStakeRightSection.background = getRoundedCornerDrawable(cornerSizeDp = 10, fillColorRes = R.color.block_background_dark)

        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(context), 8.dp(context), 16.dp(context), 0)
        }
    }

    fun setChainUi(chainUi: SyncingData<ChainUi>) {
        binder.itemDashboardHasStakeChain.setChain(chainUi.data)
        binder.itemDashboardHasStakeChainContainer.setShimmerShown(chainUi.isSyncing)
    }

    fun setRewards(rewardsState: ExtendedLoadingState<SyncingData<MaskableModel<AmountModel>>>) {
        rewardsLabelGroup.applyState(rewardsState)
        rewardsAmountGroup.applyState(rewardsState) { setMaskableText(it.maskableToken(), maskDrawableRes = R.drawable.mask_dots_big) }
        rewardsFiatGroup.applyState(rewardsState) { setMaskableText(it.maskableFiat()) }
    }

    fun setStake(stake: SyncingData<MaskableModel<AmountModel>>) {
        binder.itemDashboardHasStakeStakeAmount.setMaskableText(stake.data.maskableToken())
        binder.itemDashboardHasStakeStakeAmountContainer.setShimmerShown(stake.isSyncing)

        binder.itemDashboardHasStakeStakesFiat.setMaskableText(stake.data.maskableFiat())
        binder.itemDashboardHasStakeStakesFiatContainer.setShimmerShown(stake.isSyncing)
    }

    fun setStatus(status: ExtendedLoadingState<SyncingData<StakeStatusModel>>) {
        stakeStatusGroup.applyState(status) { setModel(it) }
    }

    fun setEarnings(earningsState: ExtendedLoadingState<SyncingData<String>>) {
        earningsGroup.applyState(earningsState) { text = it }

        binder.itemDashboardHasStakeEarningsSuffix.setVisible(earningsState.isLoaded())
    }

    fun setStakingTypeBadge(model: StakingTypeModel?) {
        binder.itemDashboardHasStakeStakingType.setModelOrHide(model)
    }

    fun unbind() {
        binder.itemDashboardHasStakeChain.unbind()
    }
}
