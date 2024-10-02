package io.novafoundation.nova.feature_assets.presentation.novacard.waiting

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.duration.CompoundDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.EstimatedDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.TimeDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.ZeroDurationFormatter
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WaitingNovaCardTopUpViewModel(
    private val assetsRouter: AssetsRouter,
    private val resourceManager: ResourceManager,
    private val novaCardInteractor: NovaCardInteractor
) : BaseViewModel() {

    init {
        novaCardInteractor.observeNovaCardState()
            .onEach { novaCardActive ->
                if (novaCardActive) {
                    assetsRouter.back()
                }
            }.launchIn(this)
    }

    fun getTimerValue(): TimerValue {
        val timeToCardCreation = novaCardInteractor.getTimeToCardCreation()
        return TimerValue(timeToCardCreation, System.currentTimeMillis())
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
