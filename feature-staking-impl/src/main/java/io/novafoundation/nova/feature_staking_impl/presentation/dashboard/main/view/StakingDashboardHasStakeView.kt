package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setShimmerShown
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.unsafeLazy
import io.novafoundation.nova.common.view.shape.getBlockDrawable
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

    private val imageLoader: ImageLoader

    private val binder = ItemDashboardHasStakeBinding.inflate(inflater(), this)

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
        imageLoader = FeatureUtils.getCommonApi(context).imageLoader()

        background = context.getBlockDrawable().withRippleMask()
        binder.itemDashboardHasStakeRightSection.background = getRoundedCornerDrawable(cornerSizeDp = 10, fillColorRes = R.color.block_background_dark)

        layoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16.dp(context), 8.dp(context), 16.dp(context), 0)
        }
    }

    fun setAssetIcon(assetIcon: SyncingData<Icon>) {
        binder.itemDashboardHasStakeAssetIcon.setIcon(assetIcon.data, imageLoader)
        binder.itemDashboardHasStakeAssetContainer.setShimmerShown(assetIcon.isSyncing)
    }

    fun setAssetLabel(assetLabel: SyncingData<String>) {
        binder.itemDashboardHasStakeRewardsLabelContainer.setShimmerShown(assetLabel.isSyncing)
        binder.itemDashboardHasStakeRewardsLabel.text = assetLabel.data
    }

    fun setRewards(rewardsState: ExtendedLoadingState<SyncingData<MaskableModel<AmountModel>>>) {
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
}
