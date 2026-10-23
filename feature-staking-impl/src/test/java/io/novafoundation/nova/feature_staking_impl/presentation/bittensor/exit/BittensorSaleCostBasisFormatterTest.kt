package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedPurchaseTotals
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapSimulation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.Before
import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorSaleCostBasisFormatterTest {

    private val resources = mock(ResourceManager::class.java)

    private val tokenFormatter = mock(TokenFormatter::class.java)

    private val fiatFormatter = mock(FiatFormatter::class.java)

    private val formatter = BittensorSaleCostBasisFormatter(resources, tokenFormatter, fiatFormatter)

    private val plainTokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence {
            val value = amount.stripTrailingZeros().toPlainString()
            return if (config.includeAssetTicker) "$value ${token.value}" else value
        }
    }

    private val profitFormatter = BittensorSaleCostBasisFormatter(resources, plainTokenFormatter, fiatFormatter)

    private val token = mock(TokenBase::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val purchase = BittensorReportedOperation(1, "hotkey", "10", "100", "0.1", "StakeAdded", "2026-10-01T12:00:00Z", "purchase", "buy")

    private val basis = BittensorEstimatedCostBasis(listOf(purchase), true)

    @Before
    fun prepareToken() {
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("NATIVE"))
        `when`(resources.getString(R.string.bittensor_avg_buy_price_per, "α")).thenReturn("per α")
        `when`(resources.getString(R.string.bittensor_avg_buy_price_from, "0.1", "α")).thenReturn("from 0.1 per α")
        `when`(resources.getString(R.string.common_not_available)).thenReturn("Not available")
        `when`(resources.getString(R.string.bittensor_no_purchases)).thenReturn("no purchases")
    }

    @Test
    fun `empty buy amount retains the known current average rather than a placeholder quote`() {
        val result = profitFormatter.projectedAveragePrice(basis, null, 1, token, "α")

        assertEquals("0.1 NATIVE", result.amount.token)
        assertEquals("per α", result.amount.fiat)
        assertEquals(R.color.text_primary, result.colorRes)
    }

    @Test
    fun `buy above current average shows an increase and the previous price`() {
        val result = profitFormatter.projectedAveragePrice(basis, buyQuote("2", "10"), 1, token, "α")

        assertTrue(result.amount.token.toString().startsWith("▲ "))
        assertEquals("from 0.1 per α", result.amount.fiat)
        assertEquals(R.color.text_negative, result.colorRes)
    }

    @Test
    fun `buy below current average shows a decrease and the previous price`() {
        val result = profitFormatter.projectedAveragePrice(basis, buyQuote("0.5", "10"), 1, token, "α")

        assertTrue(result.amount.token.toString().startsWith("▼ "))
        assertEquals("from 0.1 per α", result.amount.fiat)
        assertEquals(R.color.text_positive, result.colorRes)
    }

    @Test
    fun `buy at current average has no invented price movement`() {
        val result = profitFormatter.projectedAveragePrice(basis, buyQuote("1", "10"), 1, token, "α")

        assertEquals("0.1 NATIVE", result.amount.token)
        assertEquals(R.color.text_primary, result.colorRes)
    }

    @Test
    fun `buy average distinguishes missing history incomplete history and no purchases`() {
        val quote = buyQuote("2", "10")
        val incomplete = BittensorEstimatedCostBasis(listOf(purchase), false)
        val noPurchases = BittensorEstimatedCostBasis(emptyList(), true)

        assertEquals("—", profitFormatter.projectedAveragePrice(null, quote, 1, token, "α").amount.token)
        assertEquals("—", profitFormatter.projectedAveragePrice(incomplete, quote, 1, token, "α").amount.token)
        val empty = profitFormatter.projectedAveragePrice(noPurchases, quote, 1, token, "α").amount
        assertEquals("Not available", empty.token)
        assertEquals("no purchases", empty.fiat)
    }

    private val quote = BittensorExitQuote(
        netuid = BittensorNetuid.Subnet(1),
        amount = number(100),
        fullExit = false,
        estimatedTaoOut = number(300),
        minimumTaoOut = number(250),
        serviceFee = number(10),
        limitPrice = BigInteger.ONE
    )

    @Test
    fun `position earned retains sign native units and semantic colors`() {
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("NATIVE"))

        val gain = profitFormatter.profit(BigDecimal("1.2"), token)
        assertEquals("+1.2 NATIVE", gain.amount.token)
        assertEquals(R.color.text_positive, gain.colorRes)
        assertNull(gain.amount.fiat)

        val loss = profitFormatter.profit(BigDecimal("-1.2"), token)
        assertEquals("−1.2 NATIVE", loss.amount.token)
        assertEquals(R.color.text_negative, loss.colorRes)

        val zero = profitFormatter.profit(BigDecimal.ZERO, token)
        assertEquals("0 NATIVE", zero.amount.token)
        assertEquals(R.color.text_primary, zero.colorRes)
    }

    @Test
    fun `sale profit deducts Nova commission but not the network fee`() {
        assertEquals(number(90), estimatedSaleProfit(BittensorEstimatedPurchaseTotals(number(200), number(100)), quote))
    }

    @Test
    fun `loss and break even remain signed rather than being clamped to zero`() {
        assertEquals(number(-210), estimatedSaleProfit(BittensorEstimatedPurchaseTotals(number(500), number(100)), quote))
        assertEquals(BigInteger.ZERO, estimatedSaleProfit(BittensorEstimatedPurchaseTotals(number(290), number(100)), quote))
    }

    @Test
    fun `missing or incomplete history stays unknown`() {
        val result = formatter.averagePrice(BittensorEstimatedCostBasis(emptyList(), false), null, 1)
        assertEquals("—", result.token)
        assertNull(result.fiat)
        assertEquals("—", formatter.profit(null, null, 1).amount.token)
    }

    @Test
    fun `complete history with no purchases explains the missing average`() {
        `when`(resources.getString(R.string.common_not_available)).thenReturn("Not available")
        `when`(resources.getString(R.string.bittensor_no_purchases)).thenReturn("no purchases")

        val result = formatter.averagePrice(BittensorEstimatedCostBasis(emptyList(), true), null, 1)

        assertEquals("Not available", result.token)
        assertEquals("no purchases", result.fiat)
    }

    @Test
    fun `sale average shows no purchases only for a settled empty history`() {
        val known = profitFormatter.averagePrice(BittensorSaleCostBasis.Known(BittensorEstimatedPurchaseTotals(number(10), number(100)), false), token, "α")
        assertEquals("0.1 NATIVE", known.token)
        assertEquals("per α", known.fiat)

        val none = profitFormatter.averagePrice(BittensorSaleCostBasis.NoPurchases, token, "α")
        assertEquals("Not available", none.token)
        assertEquals("no purchases", none.fiat)

        val settling = profitFormatter.averagePrice(BittensorSaleCostBasis.Unknown, token, "α")
        assertEquals("—", settling.token)
        assertNull(settling.fiat)
    }

    @Test
    fun `sale earned needs settled totals`() {
        val totals = BittensorEstimatedPurchaseTotals(number(200), number(100))

        assertEquals("—", profitFormatter.profit(BittensorSaleCostBasis.Known(totals, false), quote, token).amount.token)
        assertEquals("—", profitFormatter.profit(BittensorSaleCostBasis.Unknown, quote, token).amount.token)
        assertEquals("—", profitFormatter.profit(BittensorSaleCostBasis.Known(totals, true), null, token).amount.token)
    }

    private fun number(value: Long): BigInteger = BigInteger.valueOf(value)

    private fun buyQuote(paid: String, received: String): BittensorSubnetBuyQuote {
        val netuid = BittensorNetuid.Subnet(1)
        val input = BigDecimal(paid).movePointRight(9).toBigIntegerExact()
        val output = BigDecimal(received).movePointRight(9).toBigIntegerExact()
        val simulation = BittensorSwapSimulation(netuid, BittensorSwapDirection.TAO_TO_ALPHA, input, output, BigInteger.ZERO, BigInteger.ZERO)
        return BittensorSubnetBuyQuote(netuid, input, BigInteger.ZERO, simulation, BigInteger.ONE, BigDecimal.ZERO, BigInteger.ONE, 0L)
    }
}
