package io.novafoundation.nova.feature_crowdloan_impl.domain.main

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import java.math.BigDecimal
import kotlin.reflect.KClass

class Crowdloan(
    val parachainMetadata: ParachainMetadata?,
    val parachainId: ParaId,
    val raisedFraction: BigDecimal,
    val state: State,
    val leasePeriodInMillis: Long,
    val leasedUntilInMillis: Long,
    val fundInfo: FundInfo,
    val myContribution: Contribution?,
) {

    sealed class State {

        companion object {

            val STATE_CLASS_COMPARATOR = Comparator<KClass<out State>> { first, _ ->
                when (first) {
                    Active::class -> -1
                    Finished::class -> 1
                    else -> 0
                }
            }
        }

        object Finished : State()

        class Active(val remainingTimeInMillis: Long) : State()
    }
}
