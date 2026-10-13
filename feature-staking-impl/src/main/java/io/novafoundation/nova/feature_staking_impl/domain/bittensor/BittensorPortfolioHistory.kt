package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Presets of `POST /v1/bittensor/portfolio/history/search`. The server fixes the window and the sample step;
 * [fiatGridPeriod] is the CoinGecko range wide enough to price every sample in the selected currency.
 */
enum class BittensorPortfolioHistoryPeriod(
    val wireName: String,
    val step: Duration,
    val duration: Duration,
    val fiatGridPeriod: PricePeriod
) {
    ONE_DAY("ONE_DAY", step = 1.hours, duration = 1.days, fiatGridPeriod = PricePeriod.DAY),
    SEVEN_DAYS("SEVEN_DAYS", step = 1.hours, duration = 7.days, fiatGridPeriod = PricePeriod.MONTH),
    THIRTY_DAYS("THIRTY_DAYS", step = 4.hours, duration = 30.days, fiatGridPeriod = PricePeriod.MONTH),
    NINETY_DAYS("NINETY_DAYS", step = 8.hours, duration = 90.days, fiatGridPeriod = PricePeriod.YEAR);

    /** A series whose first sample sits later than this after the window start does not cover the period. */
    val coverageTolerance: Duration
        get() = step * COVERAGE_STEPS

    private companion object {
        const val COVERAGE_STEPS = 2
    }
}

/** One server-sampled bucket; [timestamp] in epoch seconds, values as reported by tao.app. */
data class BittensorPortfolioStakePoint(
    val timestamp: Long,
    val taoValue: BigDecimal,
    val usdValue: BigDecimal,
    val completed: Boolean
)

/** The bounded, ascending series of one coldkey over the server-owned window (epoch seconds, inclusive). */
data class BittensorPortfolioStakeHistory(
    val period: BittensorPortfolioHistoryPeriod,
    val windowStart: Long,
    val windowEnd: Long,
    val points: List<BittensorPortfolioStakePoint>
)
