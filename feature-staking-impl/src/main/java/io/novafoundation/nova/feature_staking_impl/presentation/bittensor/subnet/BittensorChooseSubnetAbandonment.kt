package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.common.di.scope.FeatureScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The abandon tracker iOS hands from the subnet picker to the subnet details and validator list opened from it.
 * A visit is keyed by its selection flow; when the picker closes without the user moving on from any of these screens,
 * it yields one staking_abandoned for the screen shown last.
 */
@FeatureScope
class BittensorChooseSubnetAbandonment @Inject constructor() {

    private class Visit(var stage: BittensorScreen, var target: StakingAnalyticsTarget?, var proceeded: Boolean = false)

    private val visits = ConcurrentHashMap<String, Visit>()

    fun start(flowId: String) {
        visits.putIfAbsent(flowId, Visit(BittensorScreen.SUBNET_SELECT, null))
    }

    /** Does nothing outside a visit, as the screens opened elsewhere get no tracker on iOS. */
    fun becomeActive(flowId: String, stage: BittensorScreen, target: StakingAnalyticsTarget?) {
        visits[flowId]?.let { visit ->
            visit.stage = stage
            visit.target = target
        }
    }

    fun markProceeded(flowId: String) {
        visits[flowId]?.proceeded = true
    }

    fun finish(flowId: String): BittensorAnalyticsEvent.Abandoned? {
        val visit = visits.remove(flowId) ?: return null
        return if (visit.proceeded) null else BittensorAnalyticsEvent.Abandoned(visit.stage, visit.target)
    }
}
