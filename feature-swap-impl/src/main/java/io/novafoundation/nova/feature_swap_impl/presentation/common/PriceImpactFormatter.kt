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

    private val thresholdsToColors = listOf(
        priceImpactThresholds.highPriceImpact to R.color.text_negative,
        priceImpactThresholds.mediumPriceImpact to R.color.text_warning,
        priceImpactThresholds.lowPriceImpact to R.color.text_secondary,
    )

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
        return thresholdsToColors.firstOrNull { priceImpact > it.first }
            ?.second
    }
}
