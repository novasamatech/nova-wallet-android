package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeInvisible
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewUserRewardsBinding
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState.ClaimableRewards
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class UserRewardsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewUserRewardsBinding.inflate(inflater(), this, true)

    init {
        binder.userRewardsPendingContainer.background = getRoundedCornerDrawable(fillColorRes = R.color.block_background, cornerSizeDp = 10)
        binder.userRewardsPendingGroup.makeGone()
    }

    fun showPendingRewardsLoading() {
        binder.userRewardsShimmerGroup.makeVisible()
        binder.userRewardsContentGroup.makeInvisible()

        binder.userRewardsTokenAmountShimmer.startShimmer()
        binder.userRewardsFiatAmountShimmer.startShimmer()
    }

    fun setStakingPeriod(period: String) {
        binder.userRewardsStakingPeriod.text = period
    }

    fun showRewards(amountModel: AmountModel) {
        binder.userRewardsShimmerGroup.makeGone()
        binder.userRewardsContentGroup.makeVisible()

        binder.userRewardsTokenAmountShimmer.stopShimmer()
        binder.userRewardsFiatAmountShimmer.stopShimmer()

        binder.userRewardsTokenAmount.text = amountModel.token
        binder.userRewardsFiatAmount.text = amountModel.fiat
    }

    fun setBannerImage(@DrawableRes imageRes: Int) {
        binder.userRewardsBanner.setImage(imageRes)
    }

    fun setClaimClickListener(listener: (View) -> Unit) {
        binder.userRewardsPendingClaim.setOnClickListener(listener)
    }

    fun setClaimableRewardsState(pendingRewardsState: LoadingState<ClaimableRewards>?) {
        when (pendingRewardsState) {
            null, is LoadingState.Loading -> binder.userRewardsPendingGroup.makeGone()
            is LoadingState.Loaded -> {
                val claimableRewards = pendingRewardsState.data

                binder.userRewardsPendingGroup.makeVisible()

                binder.userRewardsPendingAmount.text = claimableRewards.amountModel.token
                binder.userRewardsPendingFiat.text = claimableRewards.amountModel.fiat

                binder.userRewardsPendingClaim.isEnabled = claimableRewards.canClaim
            }
        }
    }

    fun setOnRewardPeriodClickedListener(onClick: OnClickListener) {
        binder.userRewardsStakingPeriod.setOnClickListener(onClick)
    }
}
