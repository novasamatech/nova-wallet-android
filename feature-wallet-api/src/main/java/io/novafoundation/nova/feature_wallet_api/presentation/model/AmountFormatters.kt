package io.novafoundation.nova.feature_wallet_api.presentation.model

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.DimenRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.toAmountWithFraction
import io.novafoundation.nova.feature_wallet_api.R

interface AmountFormatter {

    fun formatBalanceWithFraction(unformattedAmount: CharSequence, @DimenRes floatAmountSize: Int): CharSequence
}

class RealAmountFormatter(
    private val resourceManager: ResourceManager
) : AmountFormatter {

    override fun formatBalanceWithFraction(unformattedAmount: CharSequence, @DimenRes floatAmountSize: Int): CharSequence {
        val amountWithFraction = unformattedAmount.toAmountWithFraction()

        val textColor = resourceManager.getColor(R.color.text_secondary)
        val colorSpan = ForegroundColorSpan(textColor)
        val sizeSpan = AbsoluteSizeSpan(resourceManager.getDimensionPixelSize(floatAmountSize))

        return with(amountWithFraction) {
            val spannableBuilder = SpannableStringBuilder()
                .append(amount)
            if (fraction != null) {
                spannableBuilder.append(separator + fraction)
                val startIndex = amount.length
                val endIndex = amount.length + separator.length + fraction!!.length
                spannableBuilder.setSpan(colorSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableBuilder.setSpan(sizeSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            spannableBuilder
        }
    }
}

fun CharSequence.formatBalanceWithFraction(formatter: AmountFormatter, @DimenRes floatAmountSize: Int): CharSequence {
    return formatter.formatBalanceWithFraction(this, floatAmountSize)
}
