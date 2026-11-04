package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevNotExecuted
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevCarrier
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.BittensorTransactionFailureReason
import io.novafoundation.nova.analytics.DurationBucket
import io.novafoundation.nova.analytics.SlippageBucket
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingCompletionOutcome
import io.novafoundation.nova.analytics.StakingFailureStage
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingMode
import io.novafoundation.nova.analytics.UnstakeMode
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorFailureStage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import java.io.IOException

internal fun mapBittensorOperationAnalytics(
    result: BittensorOperationResult,
    target: StakingAnalyticsTarget,
    flow: StakingFlow,
    duration: DurationBucket,
    mode: StakingMode?,
    unstakeMode: UnstakeMode?,
    hotkeys: Int?,
    hasLocked: Boolean?,
    slippage: SlippageBucket?
): AnalyticsEvent? {
    val network = BittensorAnalyticsEvent.NETWORK
    val knownFailure = result is BittensorOperationResult.Failure && when (result.stage) {
        BittensorFailureStage.NOT_SUBMITTED, BittensorFailureStage.NOT_EXECUTED -> true
        BittensorFailureStage.DISPATCHED -> result.transactionHash != null
        BittensorFailureStage.UNKNOWN -> false
    }
    if (result is BittensorOperationResult.Failure && knownFailure) {
        val shieldFailure = result.cause as? BittensorMevNotExecuted
        val notSent = result.stage == BittensorFailureStage.NOT_SUBMITTED ||
            shieldFailure?.carrier == BittensorMevCarrier.ABSENT
        val stage = if (notSent) StakingFailureStage.NOT_SUBMITTED else StakingFailureStage.DISPATCHED
        val reason = when {
            result.cause is SigningCancelledException -> BittensorTransactionFailureReason.USER_CANCELLED
            result.cause is IOException -> BittensorTransactionFailureReason.NETWORK_ERROR
            stage == StakingFailureStage.NOT_SUBMITTED -> BittensorTransactionFailureReason.NOT_SUBMITTED
            else -> BittensorTransactionFailureReason.DISPATCH_OTHER
        }
        return BittensorAnalyticsEvent.Failed(flow, network, target, mode, unstakeMode, reason, stage, true)
    }

    val outcome = if (result is BittensorOperationResult.Success) StakingCompletionOutcome.DONE else StakingCompletionOutcome.UNCONFIRMED
    return when (flow) {
        StakingFlow.STAKE -> BittensorAnalyticsEvent.Completed(network, target, requireNotNull(mode), outcome, duration, null)
        StakingFlow.UNSTAKE -> BittensorAnalyticsEvent.UnstakeCompleted(
            network, target, requireNotNull(unstakeMode), outcome, duration, null, hotkeys, requireNotNull(hasLocked), slippage
        )
        StakingFlow.CLAIM -> if (result is BittensorOperationResult.Success) {
            BittensorAnalyticsEvent.ClaimCompleted(network, null, duration, result.outcome?.tao?.isZero)
        } else {
            null
        }
    }
}
