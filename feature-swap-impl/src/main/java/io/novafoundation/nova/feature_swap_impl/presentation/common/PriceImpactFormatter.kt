package io.novafoundation.nova.feature_swap_impl.presentation.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds

interface PriceImpactFormatter {

    fun format(priceImpact: Fraction): CharSequence?

    fun formatWithBrackets(priceImpact: Fraction): CharSequence?
}

class RealPriceImpactFormatter(
    private val priceImpactThresholds: PriceImpactThresholds,
    private val resourceManager: ResourceManager
) : PriceImpactFormatter {

    override fun format(priceImpact: Fraction): CharSequence? {
        val color = priceImpactThresholds.colorFor(priceImpact) ?: return null
        return priceImpact.formatPercents().toSpannable(colorSpan(resourceManager.getColor(color)))
    }

    override fun formatWithBrackets(priceImpact: Fraction): CharSequence? {
        val color = priceImpactThresholds.colorFor(priceImpact) ?: return null

        val formattedImpact = "(${priceImpact.formatPercents()})"
        return formattedImpact.toSpannable(colorSpan(resourceManager.getColor(color)))
    }
}

internal fun PriceImpactThresholds.colorFor(priceImpact: Fraction): Int? {
    return when {
        priceImpact > highPriceImpact -> R.color.text_negative
        priceImpact > mediumPriceImpact -> R.color.text_warning
        priceImpact >= lowPriceImpact -> R.color.text_secondary
        else -> null
    }
}
