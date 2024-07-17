package io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.lessEpsilon
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve

/**
 * Stepped curve, beginning at `(0, begin)`, then remaining constant for `period`, at which
 * point it steps down to `(period, begin - step)`. It then remains constant for another
 * `period` before stepping down to `(period * 2, begin - step * 2)`. This pattern continues
 * but the `y` component has a lower limit of `end`.
 *
 * @see <a href="https://github.com/paritytech/substrate/blob/37664fe5b3513eb996225f016eceaf74963b8133/frame/referenda/src/types.rs#L269">Source</a>
 */
class SteppedDecreasingCurve(
    private val begin: Perbill,
    private val end: Perbill,
    private val step: Perbill,
    private val period: Perbill
) : VotingCurve {

    override val name: String = "SteppedDecreasing"

    override fun threshold(x: Perbill): Perbill {
        val passedPeriods = x.divideToIntegralValue(period)
        val decrease = passedPeriods * step

        return (begin - decrease).coerceIn(begin, end)
    }

    override fun delay(y: Perbill): Perbill {
        return when {
            y < end -> Perbill.ONE
            else -> period.multiply(begin - y.coerceAtMost(begin) + step.lessEpsilon()).divideToIntegralValue(step)
        }
    }
}
