package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewStakeSummaryBinding
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class StakeSummaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    sealed class Status(@StringRes val textRes: Int, @ColorRes val tintRes: Int, @DrawableRes val indicatorRes: Int) {

        object Active : Status(R.string.common_active, R.color.text_positive, R.drawable.ic_indicator_positive_pulse)

        object Inactive : Status(R.string.staking_nominator_status_inactive, R.color.text_negative, R.drawable.ic_indicator_negative_pulse)

        class Waiting(
            val timeLeft: Long,
            @StringRes customMessageFormat: Int
        ) : Status(customMessageFormat, R.color.text_secondary, R.drawable.ic_indicator_inactive_pulse)
    }

    private val binder = ViewStakeSummaryBinding.inflate(inflater(), this)

    init {
        with(context) {
            background = getBlockDrawable()
        }
    }

    fun showStakeStatus(status: Status) {
        binder.stakeSummaryStatusShimmer.makeGone()

        with(binder.stakeSummaryStatus) {
            makeVisible()

            setStatusIndicator(status.indicatorRes)
            setTextColorRes(status.tintRes)

            if (status is Status.Waiting) {
                startTimer(
                    millis = status.timeLeft,
                    customMessageFormat = status.textRes
                )
            } else {
                stopTimer()
                setText(status.textRes)
            }
        }
    }

    fun showStakeAmount(amountModel: AmountModel) {
        binder.stakeSummaryTokenStake.makeVisible()
        binder.stakeSummaryFiatStake.makeVisible()

        binder.stakeSummaryFiatStakeShimmer.makeGone()
        binder.stakeSummaryTokenStakeShimmer.makeGone()

        binder.stakeSummaryTokenStake.text = amountModel.token
        binder.stakeSummaryFiatStake.text = amountModel.fiat
    }

    fun showLoading() {
        binder.stakeSummaryShimmerGroup.makeVisible()
        binder.stakeSummaryContentGroup.makeGone()
    }

    fun setStatusClickListener(listener: OnClickListener) {
        binder.stakeSummaryStatus.setOnClickListener(listener)
    }
}
