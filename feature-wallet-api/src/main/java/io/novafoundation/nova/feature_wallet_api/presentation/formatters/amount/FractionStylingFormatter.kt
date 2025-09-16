package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.toAmountWithFraction
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionPartStyling

interface FractionStylingFormatter {

    fun formatFraction(
        unformattedAmount: CharSequence,
        @DimenRes floatAmountSize: Int,
        @ColorRes textColorRes: Int
    ): CharSequence
}

class RealFractionStylingFormatter(
    private val resourceManager: ResourceManager
) : FractionStylingFormatter {

    override fun formatFraction(
        unformattedAmount: CharSequence,
        @DimenRes floatAmountSize: Int,
        @ColorRes textColorRes: Int
    ): CharSequence {
        val amountWithFraction = unformattedAmount.toAmountWithFraction()

        val textColor = resourceManager.getColor(textColorRes)
        val colorSpan = ForegroundColorSpan(textColor)
        val sizeSpan = AbsoluteSizeSpan(resourceManager.getDimensionPixelSize(floatAmountSize))

        return with(amountWithFraction) {
            val decimalAmount = amountWithFraction.amount

            val spannableBuilder = SpannableStringBuilder()
                .append(decimalAmount)
            if (fraction != null) {
                spannableBuilder.append(separator + fraction)
                val startIndex = decimalAmount.length
                val endIndex = decimalAmount.length + separator.length + fraction!!.length
                spannableBuilder.setSpan(colorSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableBuilder.setSpan(sizeSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            spannableBuilder
        }
    }
}

fun CharSequence.applyFractionStyling(fractionStylingFormatter: FractionStylingFormatter, fractionPartStyling: FractionPartStyling): CharSequence {
    return when (fractionPartStyling) {
        FractionPartStyling.NoStyle -> this
        is FractionPartStyling.Styled -> fractionStylingFormatter.formatFraction(this, fractionPartStyling.sizeRes, fractionPartStyling.colorRes)
    }
}
