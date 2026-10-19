package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal

data class BittensorPortfolioHistoryRequest(
    val metaId: Long,
    val chainId: String,
    val nativePriceId: String?,
    val currency: Currency,
    val period: BittensorPortfolioPeriod
)

data class BittensorPortfolioHistory(
    val account: BittensorAccountHistory,
    // Null means reported USD; an empty list means no usable FX observations.
    val nativePrices: List<HistoricalCoinRate>?
)

/** Complete response for a bounded window of partial tao.app-reported history, not a wallet ledger. */
data class BittensorAccountHistory(
    val period: BittensorPortfolioPeriod,
    val windowStart: Long,
    val windowEnd: Long,
    val points: List<BittensorPortfolioHistoryPoint>,
    val status: BittensorPortfolioHistoryStatus
)

data class BittensorPortfolioHistoryStatus(val asOf: BigDecimal, val stale: Boolean)

data class BittensorPortfolioHistoryPoint(val timestamp: Long, val tao: BigDecimal, val usd: BigDecimal, val completed: Boolean)

data class BittensorPortfolioValuePoint(val timestamp: Long, val tao: BigDecimal, val fiat: BigDecimal)

data class BittensorPortfolioValueSeries(val points: List<BittensorPortfolioValuePoint>, val change: BigDecimal?)
