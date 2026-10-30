package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters

import android.widget.TextView
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.math.BigDecimal

class RealPricePriceTextInjectorTest {

    private val currency = mock(Currency::class.java)

    private val formattedAmounts = mutableListOf<Pair<BigDecimal, Currency>>()

    private val fiatFormatter = object : FiatFormatter {
        override fun formatFiat(fiatAmount: BigDecimal, currency: Currency, config: FiatConfig): CharSequence {
            formattedAmounts += fiatAmount to currency
            return "formatted:$fiatAmount"
        }
    }

    private val textView = mock(TextView::class.java)

    @Test
    fun `selected entry formats its historical price`() {
        val price = 12.5f
        val amount = price.toBigDecimal()
        val injector = RealPricePriceTextInjector(currency, BigDecimal.TEN, fiatFormatter)

        injector.format(price, textView, isEntrySelected = true)

        assertEquals(listOf(amount to currency), formattedAmounts)
        verify(textView).setText("formatted:$amount")
    }

    @Test
    fun `unselected entry formats latest coin rate`() {
        val latestPrice = BigDecimal.TEN
        val injector = RealPricePriceTextInjector(currency, latestPrice, fiatFormatter)

        injector.format(12.5f, textView, isEntrySelected = false)

        assertEquals(listOf(latestPrice to currency), formattedAmounts)
        verify(textView).setText("formatted:$latestPrice")
    }

    @Test
    fun `missing latest rate falls back to the chart entry price`() {
        val price = 12.5f
        val amount = price.toBigDecimal()
        val injector = RealPricePriceTextInjector(currency, null, fiatFormatter)

        injector.format(price, textView, isEntrySelected = false)

        assertEquals(listOf(amount to currency), formattedAmounts)
        verify(textView).setText("formatted:$amount")
    }
}
