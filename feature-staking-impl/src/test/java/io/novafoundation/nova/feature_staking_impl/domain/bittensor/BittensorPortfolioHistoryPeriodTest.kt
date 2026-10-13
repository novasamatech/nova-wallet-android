package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class BittensorPortfolioHistoryPeriodTest {

    @Test
    fun `coverage tolerance is two server steps`() {
        assertEquals(2.hours, BittensorPortfolioHistoryPeriod.ONE_DAY.coverageTolerance)
        assertEquals(2.hours, BittensorPortfolioHistoryPeriod.SEVEN_DAYS.coverageTolerance)
        assertEquals(8.hours, BittensorPortfolioHistoryPeriod.THIRTY_DAYS.coverageTolerance)
        assertEquals(16.hours, BittensorPortfolioHistoryPeriod.NINETY_DAYS.coverageTolerance)
    }

    @Test
    fun `wire names match the backend enum in preset order`() {
        assertEquals(
            listOf("ONE_DAY", "SEVEN_DAYS", "THIRTY_DAYS", "NINETY_DAYS"),
            BittensorPortfolioHistoryPeriod.entries.map { it.wireName }
        )
    }

    @Test
    fun `fiat grid source covers the whole window`() {
        BittensorPortfolioHistoryPeriod.entries.forEach { period ->
            val source = when (period.fiatGridPeriod) {
                PricePeriod.DAY -> 1.days
                PricePeriod.WEEK -> 7.days
                PricePeriod.MONTH -> 30.days
                PricePeriod.YEAR -> 365.days
                PricePeriod.MAX -> Int.MAX_VALUE.days
            }
            assert(source >= period.duration) { "$period grid ${period.fiatGridPeriod} shorter than ${period.duration}" }
        }
    }
}
