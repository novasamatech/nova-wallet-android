package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class BittensorAmountFormatterTest {

    private val resources = mock(ResourceManager::class.java)

    private val fiatFormatter = mock(FiatFormatter::class.java)

    private val currency = mock(Currency::class.java)

    private val tokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence = "${amount.toPlainString()} ${token.value}"
    }

    private val formatter = BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    @Test
    fun `an unknown fiat valuation stays absent without formatting an invented zero`() {
        val amount = formatter.amount(BigDecimal.ONE, TokenSymbol("CUSTOM"), null, currency)

        assertEquals("1 CUSTOM", amount.token)
        assertNull(amount.fiat)
        verifyNoInteractions(fiatFormatter)
    }
}
