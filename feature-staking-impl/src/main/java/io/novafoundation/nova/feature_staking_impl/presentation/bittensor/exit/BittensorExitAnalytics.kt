package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.SlippageBucket
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.analytics.UnstakeMode
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novasama.substrate_sdk_android.hash.isPositive

/** What the unstake events report about an exit, when it is initiated and when it finishes. */
internal class BittensorExitAnalyticsValues(position: BittensorExitPosition, quote: BittensorExitQuote) {

    val target = bittensorExitAnalyticsTarget(position.isRoot)

    val mode = if (quote.fullExit) UnstakeMode.FULL else UnstakeMode.PARTIAL

    val hotkeys = quote.groupHotkeys.size.takeIf { it > 0 } ?: 1

    val hasLocked = position.preflight.availability.locked.isPositive()

    val slippage = quote.slippageFraction.takeUnless { position.isRoot }?.let { SlippageBucket.from(it.movePointRight(2)) }
}

internal fun bittensorExitAnalyticsTarget(isRoot: Boolean) = if (isRoot) StakingAnalyticsTarget.ROOT else StakingAnalyticsTarget.SUBNET

/** Reports an exit validation alert by how it ended; failures without an analytics kind are not reported. */
internal fun AnalyticsService.trackExitValidation(
    failure: BittensorExitValidationFailure,
    stage: StakingValidationStage,
    outcome: StakingValidationOutcome,
    isRoot: Boolean
) {
    val kind = failure.analyticsKind() ?: return
    track(BittensorAnalyticsEvent.ValidationShown(StakingFlow.UNSTAKE, kind, stage, outcome, bittensorExitAnalyticsTarget(isRoot)))
}
