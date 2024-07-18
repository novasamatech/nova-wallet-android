package io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve

import io.novafoundation.nova.common.data.network.runtime.binding.FixedI64
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.coerceInOrNull
import io.novafoundation.nova.common.utils.divideOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

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

    override fun delay(y: Perbill): Perbill {
        val maybeTerm = factor.divideOrNull(y - yOffset, MathContext(MathContext.DECIMAL64.precision, RoundingMode.HALF_UP))

        return maybeTerm?.let { it - xOffset }
            ?.coerceInOrNull(BigDecimal.ZERO, BigDecimal.ONE)
            ?: Perbill.ONE
    }
}
