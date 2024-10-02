package io.novafoundation.nova.feature_assets.presentation.novacard.waiting

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.duration.CompoundDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.EstimatedDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.TimeDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.ZeroDurationFormatter
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import kotlin.time.Duration.Companion.minutes

const val TIMER_MINUTES = 5

class WaitingNovaCardTopUpViewModel(
    private val assetsRouter: AssetsRouter,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    fun getTimerValue(): TimerValue {
        return TimerValue(TIMER_MINUTES.minutes.inWholeMilliseconds, System.currentTimeMillis())
    }

    fun getTimerFormatter(): EstimatedDurationFormatter {
        val timeDurationFormatter = TimeDurationFormatter(useHours = false)
        val compoundFormatter = CompoundDurationFormatter(
            timeDurationFormatter,
            ZeroDurationFormatter(timeDurationFormatter)
        )

        return EstimatedDurationFormatter(compoundFormatter)
    }

    fun timerFinished() {
        showError(
            resourceManager.getString(R.string.common_unexpected_error),
            resourceManager.getString(R.string.fragment_waiting_top_up_time_out_error)
        )

        assetsRouter.back()
    }
}
