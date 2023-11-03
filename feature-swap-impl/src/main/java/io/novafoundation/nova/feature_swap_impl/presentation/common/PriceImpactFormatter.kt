package io.novafoundation.nova.feature_swap_impl.presentation.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_swap_impl.R

private val THRESHOLDS = listOf(
    15.0 to R.color.text_negative,
    5.0 to R.color.text_warning,
    1.0 to R.color.text_secondary,
)

interface PriceImpactFormatter {

    fun format(priceImpact: Percent?): CharSequence?

    fun formatWithBrackets(priceImpact: Percent?): CharSequence?
}

class RealPriceImpactFormatter(private val resourceManager: ResourceManager) : PriceImpactFormatter {

    override fun format(priceImpact: Percent?): CharSequence? {
        if (priceImpact == null) return null
        val color = getColor(priceImpact) ?: return null

        return priceImpact.format().toSpannable(colorSpan(resourceManager.getColor(color)))
    }

    override fun formatWithBrackets(priceImpact: Percent?): CharSequence? {
        if (priceImpact == null) return null
        val color = getColor(priceImpact) ?: return null

        val formattedImpact = "(${priceImpact.format()})"
        return formattedImpact.toSpannable(colorSpan(resourceManager.getColor(color)))
    }

    private fun getColor(priceImpact: Percent): Int? {
        return THRESHOLDS.firstOrNull { priceImpact.value > it.first }
            ?.second
    }
}
