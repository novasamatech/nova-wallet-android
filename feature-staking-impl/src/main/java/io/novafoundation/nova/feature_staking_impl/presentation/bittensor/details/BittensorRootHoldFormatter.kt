package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootHold
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.milliseconds

data class BittensorRootHoldUi(val timeLeft: String, val description: String)

class BittensorRootHoldFormatter @Inject constructor(
    private val resources: ResourceManager,
    @Named("BittensorWithdrawalDuration") private val durationFormatter: DurationFormatter
) {

    fun format(hold: BittensorRootHold): BittensorRootHoldUi {
        val remaining = durationFormatter.format(hold.remainingMillis.toLong().milliseconds)
        val interval = durationFormatter.format(hold.intervalMillis.toLong().milliseconds)
        return BittensorRootHoldUi(
            timeLeft = resources.getString(R.string.bittensor_root_hold_time_left, remaining),
            description = resources.getString(R.string.bittensor_root_hold_description, interval)
        )
    }
}
