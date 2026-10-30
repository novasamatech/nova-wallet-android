package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class BittensorChartAxisFormatterTest {

    private val currency = mock(Currency::class.java)

    private val configuration = mock(Chain.Asset::class.java)

    private val fiatCalls = mutableListOf<Pair<BigDecimal, Currency>>()

    private val tokenCalls = mutableListOf<Pair<BigDecimal, TokenSymbol>>()

    private val fiatFormatter = object : FiatFormatter {
        override fun formatFiat(fiatAmount: BigDecimal, currency: Currency, config: FiatConfig): CharSequence {
            fiatCalls += fiatAmount to currency
            return "fiat:$fiatAmount"
        }
    }

    private val tokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence {
            tokenCalls += amount to token
            return "token:${token.value}:$amount"
        }
    }

    private val token = object : TokenBase {
        override val currency = this@BittensorChartAxisFormatterTest.currency

        override val coinRate: CoinRate? = null

        override val configuration = this@BittensorChartAxisFormatterTest.configuration
    }

    private val formatterFactory = BittensorChartAxisFormatter.Factory(fiatFormatter, tokenFormatter)

    @Test
    fun `fiat axis delegates to fiat formatter with the token currency`() {
        val labelFormatter = formatterFactory.create(token, isFiatMode = true)

        assertEquals("fiat:12.5", labelFormatter.format(12.5f))
        assertEquals(listOf(BigDecimal("12.5") to currency), fiatCalls)
        assertTrue(tokenCalls.isEmpty())
    }

    @Test
    fun `native axis uses the configured token symbol`() {
        val symbol = TokenSymbol("CUSTOM")
        `when`(configuration.symbol).thenReturn(symbol)
        val labelFormatter = formatterFactory.create(token, isFiatMode = false)

        assertEquals("token:CUSTOM:12.5", labelFormatter.format(12.5f))
        assertEquals(listOf(BigDecimal("12.5") to symbol), tokenCalls)
        assertTrue(fiatCalls.isEmpty())
    }

    @Test
    fun `switching unit does not convert an already denominated chart value`() {
        `when`(configuration.symbol).thenReturn(TokenSymbol("CUSTOM"))
        val nativeLabels = formatterFactory.create(token, isFiatMode = false)
        val fiatLabels = formatterFactory.create(token, isFiatMode = true)

        nativeLabels.format(0.25f)
        fiatLabels.format(0.25f)

        assertEquals(BigDecimal("0.25"), tokenCalls.single().first)
        assertEquals(BigDecimal("0.25"), fiatCalls.single().first)
    }
}
