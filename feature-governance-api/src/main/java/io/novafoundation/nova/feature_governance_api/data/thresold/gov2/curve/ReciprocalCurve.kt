package io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve

import io.novafoundation.nova.common.data.network.runtime.binding.FixedI64
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import java.math.MathContext

/**
 * A reciprocal (`K/(x+S)-T`) curve: `factor` is `K` and `xOffset` is `S`, `yOffset` is `T`.
 *
 * @see <a href="https://github.com/paritytech/substrate/blob/37664fe5b3513eb996225f016eceaf74963b8133/frame/referenda/src/types.rs#L271">Source</a>
 */
class ReciprocalCurve(
    private val factor: FixedI64,
    private val xOffset: FixedI64,
    private val yOffset: FixedI64
) : VotingCurve {

    override val name: String = "Reciprocal"

    override fun threshold(x: Perbill): Perbill {
        return factor.divide(x + xOffset, MathContext.DECIMAL64) + yOffset
    }
}
