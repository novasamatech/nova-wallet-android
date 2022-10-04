package io.novafoundation.nova.feature_governance_impl.data.model.curve

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import java.math.MathContext

/**
 * A reciprocal (`K/(x+S)-T`) curve: `factor` is `K` and `xOffset` is `S`, `yOffset` is `T`.
 *
 * @see <a href="https://github.com/paritytech/substrate/blob/37664fe5b3513eb996225f016eceaf74963b8133/frame/referenda/src/types.rs#L271">Source</a>
 */
class ReciprocalCurve(
    private val factor: Perbill,
    private val xOffset: Perbill,
    private val yOffset: Perbill
) : VotingCurve {

    override fun threshold(x: Perbill): Perbill {
        return factor.divide(x + xOffset, MathContext.DECIMAL64) - yOffset
    }
}
