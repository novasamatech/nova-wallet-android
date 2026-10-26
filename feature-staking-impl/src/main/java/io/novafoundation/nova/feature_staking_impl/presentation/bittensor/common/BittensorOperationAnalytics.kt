package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.DurationBucket
import io.novafoundation.nova.analytics.SlippageBucket
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingMode
import io.novafoundation.nova.analytics.UnstakeMode
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** One instance per confirmation screen; each submission attempt starts a new measurement. */
class BittensorOperationAnalytics @Inject constructor(private val service: AnalyticsService) {

    private val operationStartedAt = AtomicReference<Long?>(null)

    fun startOperation() {
        operationStartedAt.set(System.nanoTime())
    }

    fun outcome(
        result: BittensorOperationResult,
        target: StakingAnalyticsTarget,
        flow: StakingFlow,
        mode: StakingMode? = null,
        unstakeMode: UnstakeMode? = null,
        hotkeys: Int? = null,
        hasLocked: Boolean? = null,
        slippage: SlippageBucket? = null
    ) {
        val started = operationStartedAt.getAndSet(null) ?: return
        val duration = DurationBucket.from((System.nanoTime() - started) / 1_000_000)
        mapBittensorOperationAnalytics(result, target, flow, duration, mode, unstakeMode, hotkeys, hasLocked, slippage)
            ?.let(service::track)
    }
}
