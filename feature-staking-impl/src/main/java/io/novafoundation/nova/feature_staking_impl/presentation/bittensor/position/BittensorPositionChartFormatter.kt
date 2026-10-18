package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.ChartAxisFormatter

class BittensorPositionChartFormatter(
    private val tokenFormatter: TokenFormatter,
    private val symbol: TokenSymbol
) : ChartAxisFormatter {

    override fun format(value: Float): String = tokenFormatter.formatToken(value.toBigDecimal(), symbol).toString()
}
