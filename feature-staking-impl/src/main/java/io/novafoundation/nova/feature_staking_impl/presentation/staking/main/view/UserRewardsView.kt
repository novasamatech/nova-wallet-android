package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeInvisible
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState.ClaimableRewards
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsContentGroup
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsFiatAmount
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsFiatAmountShimmer
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsPendingAmount
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsPendingClaim
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsPendingContainer
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsPendingFiat
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsPendingGroup
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsShimmerGroup
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsStakingPeriod
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsTokenAmount
import kotlinx.android.synthetic.main.view_user_rewards.view.userRewardsTokenAmountShimmer

class UserRewardsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_user_rewards, this)

        // TODO apply blur
        userRewardsPendingContainer.background = getRoundedCornerDrawable(fillColorRes = R.color.block_background, cornerSizeDp = 10)
    }

    fun showPendingRewardsLoading() {
        userRewardsShimmerGroup.makeVisible()
        userRewardsContentGroup.makeInvisible()

        userRewardsTokenAmountShimmer.startShimmer()
        userRewardsFiatAmountShimmer.startShimmer()
    }

    fun setStakingPeriod(period: String) {
        userRewardsStakingPeriod.text = period
    }

    fun showRewards(amountModel: AmountModel) {
        userRewardsShimmerGroup.makeGone()
        userRewardsContentGroup.makeVisible()

        userRewardsTokenAmountShimmer.stopShimmer()
        userRewardsFiatAmountShimmer.stopShimmer()

        userRewardsTokenAmount.text = amountModel.token
        userRewardsFiatAmount.text = amountModel.fiat
    }

    fun setClaimClickListener(listener: (View) -> Unit) {
        userRewardsPendingClaim.setOnClickListener(listener)
    }

    fun setClaimableRewardsState(pendingRewardsState: LoadingState<ClaimableRewards>?) {
        when(pendingRewardsState) {
            null, is LoadingState.Loading -> userRewardsPendingGroup.makeGone()
            is LoadingState.Loaded -> {
                val claimableRewards = pendingRewardsState.data

                userRewardsPendingGroup.makeVisible()

                userRewardsPendingAmount.text = claimableRewards.amountModel.token
                userRewardsPendingFiat.text = claimableRewards.amountModel.fiat

                userRewardsPendingClaim.isEnabled = claimableRewards.canClaim
            }
        }
    }

    fun setOnRewardPeriodClickedListener(onClick: OnClickListener) {
        userRewardsStakingPeriod.setOnClickListener(onClick)
    }
}
