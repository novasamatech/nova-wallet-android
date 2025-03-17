package io.novafoundation.nova.feature_assets.presentation.novacard.waiting

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.duration.CompoundDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.EstimatedDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.TimeDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.ZeroDurationFormatter
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.waiting.flows.TopUpCaseFactory

class WaitingNovaCardTopUpViewModel(
    private val assetsRouter: AssetsRouter,
    private val novaCardInteractor: NovaCardInteractor,
    private val topUpCaseFactory: TopUpCaseFactory
) : BaseViewModel() {

    private val topUpCase = topUpCaseFactory.create(novaCardInteractor.getNovaCardState())

    val titleFlow = topUpCase.titleFlow

    init {
        topUpCase.init(this)
    }

    fun getTimerValue(): TimerValue {
        val estimatedTopUpDuration = novaCardInteractor.getEstimatedTopUpDuration()
        return TimerValue(estimatedTopUpDuration, System.currentTimeMillis())
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
        topUpCase.onTimeFinished(this)

        assetsRouter.back()
    }
}
