package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.DimenRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatWithFullAmount
import io.novafoundation.nova.common.utils.formatting.toAmountWithFraction
import io.novafoundation.nova.common.utils.withTokenSymbol
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrencyNoAbbreviation
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import java.math.BigDecimal

interface AmountFormatter {
    fun formatBalanceWithFraction(unformattedAmount: CharSequence, @DimenRes floatAmountSize: Int): CharSequence

    fun formatAmountToAmountModel(amount: BigDecimal, token: TokenBase, config: AmountConfig = AmountConfig()): AmountModel
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

    override fun formatAmountToAmountModel(
        amount: BigDecimal,
        token: TokenBase,
        config: AmountConfig
    ): AmountModel {
        return AmountModel(
            token = config.tokenAmountSign.signSymbol + formatAmount(config, amount, token),
            fiat = formatFiat(amount, token, config)
        )
    }

    private fun formatAmount(
        config: AmountConfig,
        amount: BigDecimal,
        token: TokenBase
    ): String {
        val unsignedTokenAmount = if (config.useAbbreviation) {
            if (config.includeAssetTicker) {
                amount.formatTokenAmount(token.configuration, config.roundingMode)
            } else {
                amount.format(config.roundingMode)
            }
        } else {
            val unformattedAmount = amount.formatWithFullAmount()

            if (config.includeAssetTicker) {
                unformattedAmount.withTokenSymbol(token.configuration.symbol)
            } else {
                unformattedAmount
            }
        }
        return unsignedTokenAmount
    }

    private fun formatFiat(
        amount: BigDecimal,
        token: TokenBase,
        config: AmountConfig
    ): String? {
        val fiatAmount = token.amountToFiat(amount)
            .takeIf { it != BigDecimal.ZERO || config.includeZeroFiat }

        var formattedFiat = if (config.useAbbreviation) {
            fiatAmount?.formatAsCurrency(token.currency, config.roundingMode)
        } else {
            fiatAmount?.formatAsCurrencyNoAbbreviation(token.currency)
        }

        if (config.estimatedFiat && formattedFiat != null) {
            formattedFiat = "~$formattedFiat"
        }

        return formattedFiat
    }
}
