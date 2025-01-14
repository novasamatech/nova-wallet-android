package io.novafoundation.nova.feature_swap_impl.presentation.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_swap_impl.R

private val THRESHOLDS = listOf(
    15.0.percents to R.color.text_negative,
    5.0.percents to R.color.text_warning,
    1.0.percents to R.color.text_secondary,
)

interface PriceImpactFormatter {

    fun format(priceImpact: Fraction): CharSequence?

    fun formatWithBrackets(priceImpact: Fraction): CharSequence?
}

class RealPriceImpactFormatter(private val resourceManager: ResourceManager) : PriceImpactFormatter {

    override fun format(priceImpact: Fraction): CharSequence? {
        val color = getColor(priceImpact) ?: return null
        return priceImpact.formatPercents().toSpannable(colorSpan(resourceManager.getColor(color)))
    }

    override fun formatWithBrackets(priceImpact: Fraction): CharSequence? {
        val color = getColor(priceImpact) ?: return null

        val formattedImpact = "(${priceImpact.formatPercents()})"
        return formattedImpact.toSpannable(colorSpan(resourceManager.getColor(color)))
    }

    private fun getColor(priceImpact: Fraction): Int? {
        return THRESHOLDS.firstOrNull { priceImpact > it.first }
            ?.second
    }
}
