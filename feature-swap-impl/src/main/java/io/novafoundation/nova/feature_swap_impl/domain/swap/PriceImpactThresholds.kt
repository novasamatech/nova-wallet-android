package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.utils.Fraction

class PriceImpactThresholds(
    val lowPriceImpact: Fraction,
    val mediumPriceImpact: Fraction,
    val highPriceImpact: Fraction,
    /** At or above this the swap is refused outright - too much would be lost to let a dismissible warning decide. */
    val maxAllowedPriceImpact: Fraction
)
