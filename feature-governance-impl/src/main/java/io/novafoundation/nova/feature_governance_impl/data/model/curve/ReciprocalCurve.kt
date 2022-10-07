package io.novafoundation.nova.feature_governance_impl.data.model.curve

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import java.math.BigInteger
import java.math.MathContext

/**
 * A reciprocal (`K/(x+S)-T`) curve: `factor` is `K` and `xOffset` is `S`, `yOffset` is `T`.
 *
 * @see <a href="https://github.com/paritytech/substrate/blob/37664fe5b3513eb996225f016eceaf74963b8133/frame/referenda/src/types.rs#L271">Source</a>
 */
class ReciprocalCurve(
    private val factor: BigInteger,
    private val xOffset: BigInteger,
    private val yOffset: BigInteger
) : VotingCurve {

    override fun threshold(x: Perbill): Perbill {
        val factorD = factor.toBigDecimal()
        val xOffsetD = xOffset.toBigDecimal()
        val yOffsetD = yOffset.toBigDecimal()

        return factorD.divide(x + xOffsetD, MathContext.DECIMAL64) - yOffsetD
    }
}
