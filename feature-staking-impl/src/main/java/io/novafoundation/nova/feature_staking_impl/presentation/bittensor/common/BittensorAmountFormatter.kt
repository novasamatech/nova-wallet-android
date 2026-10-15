package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal
import java.math.RoundingMode

class BittensorAmountFormatter(
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    private val resources: ResourceManager
) {

    fun amount(amount: BigDecimal, symbol: TokenSymbol, fiat: BigDecimal?, currency: Currency): AmountModel {
        return AmountModel(
            token = tokenFormatter.formatToken(amount, symbol),
            fiat = fiat?.let { fiatFormatter.formatFiat(it, currency) }
        )
    }

    fun fee(fee: Fee, token: TokenBase): BittensorFeeUi {
        val fiat = token.toFiatOrNull(fee.decimalAmount)
        return BittensorFeeUi(
            amount = tokenFormatter.formatToken(fee.decimalAmount, fee.asset.symbol).toString(),
            fiatAmount = fiat?.let { fiatFormatter.formatFiat(it, token.currency).toString() } ?: "—"
        )
    }

    fun apy(annualPercentageYield: BigDecimal?, symbol: TokenSymbol): String {
        val apy = annualPercentageYield?.toStripTrailingZerosString() ?: return "—"
        return resources.getString(R.string.bittensor_apy_paid_in, "$apy%", symbol.value)
    }

    fun exchangeRate(quote: BittensorSubnetBuyQuote, subnet: BittensorSubnet, token: TokenBase): String {
        val output = BigDecimal(quote.alphaOutInPlanks, BITTENSOR_ALPHA_DECIMALS)
        val input = token.amountFromPlanks(quote.totalTaoInPlanks)
        val rate = output.divide(input, BITTENSOR_ALPHA_DECIMALS, RoundingMode.DOWN)
        val nativeAmount = tokenFormatter.formatToken(BigDecimal.ONE, token.configuration.symbol)
        val alphaAmount = tokenFormatter.formatToken(rate, TokenSymbol(subnet.symbol.ifBlank { "—" }))
        return "$nativeAmount ≈ \u200e$alphaAmount"
    }
}
