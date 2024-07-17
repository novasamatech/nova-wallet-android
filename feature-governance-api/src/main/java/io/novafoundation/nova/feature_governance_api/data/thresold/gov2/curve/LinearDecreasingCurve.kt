package io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import java.math.RoundingMode

/**
 * Linear curve starting at `(0, ceil)`, proceeding linearly to `(length, floor)`, then
 * remaining at `floor` until the end of the period.
 *
 * @see <a href="https://github.com/paritytech/substrate/blob/37664fe5b3513eb996225f016eceaf74963b8133/frame/referenda/src/types.rs#L264">Source</a>
 */
class LinearDecreasingCurve(
    private val length: Perbill,
    private val floor: Perbill,
    private val ceil: Perbill
) : VotingCurve {

    override val name: String = "LinearDecreasing"

    override fun threshold(x: Perbill): Perbill {
        return ceil - x.coerceAtMost(length).divide(length, RoundingMode.DOWN) * (ceil - floor)
    }

    override fun delay(y: Perbill): Perbill {
        return when {
            y < floor -> Perbill.ONE
            y > ceil -> Perbill.ZERO
            else -> (ceil - y).divide(ceil - floor, RoundingMode.UP).multiply(length)
        }
    }
}
