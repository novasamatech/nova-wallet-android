package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.ChartAxisFormatter

class BittensorChartAxisFormatter(
    private val fiatFormatter: FiatFormatter,
    private val tokenFormatter: TokenFormatter,
    private val token: TokenBase,
    private val isFiatMode: Boolean
) : ChartAxisFormatter {

    override fun format(value: Float): String {
        val amount = value.toBigDecimal()
        return if (isFiatMode) {
            fiatFormatter.formatFiat(amount, token.currency).toString()
        } else {
            tokenFormatter.formatToken(amount, token.configuration.symbol).toString()
        }
    }

    class Factory(
        private val fiatFormatter: FiatFormatter,
        private val tokenFormatter: TokenFormatter
    ) {

        fun create(token: TokenBase, isFiatMode: Boolean): ChartAxisFormatter =
            BittensorChartAxisFormatter(fiatFormatter, tokenFormatter, token, isFiatMode)
    }
}
