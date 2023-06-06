package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setCompoundDrawableTint
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryContentGroup
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryFiatStake
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryFiatStakeShimmer
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryShimmerGroup
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryStatus
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryStatusShimmer
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryTokenStake
import kotlinx.android.synthetic.main.view_stake_summary.view.stakeSummaryTokenStakeShimmer

class StakeSummaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    sealed class Status(@StringRes val textRes: Int, @ColorRes val tintRes: Int) {

        object Active : Status(R.string.common_active, R.color.text_positive)

        object Inactive : Status(R.string.staking_nominator_status_inactive, R.color.text_secondary)

        class Waiting(
            val timeLeft: Long,
            @StringRes customMessageFormat: Int
        ) : Status(customMessageFormat, R.color.text_secondary)
    }

    init {
        View.inflate(context, R.layout.view_stake_summary, this)

        with(context) {
            background = getBlockDrawable()
            stakeSummaryStatus.background = addRipple(getRoundedCornerDrawable(fillColorRes = R.color.block_background))
        }
    }

    fun showStakeStatus(status: Status) {
        stakeSummaryStatusShimmer.makeGone()

        with(stakeSummaryStatus) {
            makeVisible()

            setCompoundDrawableTint(status.tintRes)
            setTextColorRes(status.tintRes)

            if (status is Status.Waiting) {
                stakeSummaryStatus.startTimer(
                    millis = status.timeLeft,
                    customMessageFormat = status.textRes
                )
            } else {
                stakeSummaryStatus.stopTimer()
                stakeSummaryStatus.setText(status.textRes)
            }
        }
    }

    fun showStakeAmount(amountModel: AmountModel) {
        stakeSummaryTokenStake.makeVisible()
        stakeSummaryFiatStake.makeVisible()

        stakeSummaryFiatStakeShimmer.makeGone()
        stakeSummaryTokenStakeShimmer.makeGone()

        stakeSummaryTokenStake.text = amountModel.token
        stakeSummaryFiatStake.text = amountModel.fiat
    }

    fun showLoading() {
        stakeSummaryShimmerGroup.makeVisible()
        stakeSummaryContentGroup.makeGone()
    }

    fun setStatusClickListener(listener: OnClickListener) {
        stakeSummaryStatus.setOnClickListener(listener)
    }
}
