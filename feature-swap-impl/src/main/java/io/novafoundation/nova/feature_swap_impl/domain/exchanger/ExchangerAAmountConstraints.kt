package io.novafoundation.nova.feature_swap_impl.domain.exchanger

import io.novafoundation.nova.common.view.input.DecimalInputConstraints
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/** Input limits shared by Exchanger A amount fields and quote requests. */
class ExchangerAAmountConstraints {

    private val limits = DecimalInputConstraints.Bounded(maxFractionDigits = 8, maxIntegerDigits = 15)

    fun forAsset(asset: Chain.Asset): DecimalInputConstraints.Bounded = limits.copy(
        maxFractionDigits = minOf(limits.maxFractionDigits, asset.precision.value)
    )
}

class ExchangerAAmountPrecisionExceeded(val constraints: DecimalInputConstraints.Bounded) :
    IllegalArgumentException("Amount exceeds Exchanger A digit limits")
