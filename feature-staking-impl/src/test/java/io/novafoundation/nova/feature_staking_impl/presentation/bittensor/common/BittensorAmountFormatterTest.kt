package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class BittensorAmountFormatterTest {

    private val resources = mock(ResourceManager::class.java)

    private val fiatFormatter = mock(FiatFormatter::class.java)

    private val currency = mock(Currency::class.java)

    private val tokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence = "${amount.toPlainString()} ${token.value}"
    }

    private val formatter = BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    private val asset = mock(Asset::class.java)

    private val token = mock(Token::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val fee = mock(Fee::class.java)

    @Before
    fun prepareAsset() {
        `when`(asset.token).thenReturn(token)
        `when`(asset.transferableInPlanks).thenReturn(BigInteger("10000000000"))
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("NATIVE"))
        `when`(fee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger("1000000"))
        `when`(resources.getString(R.string.bittensor_fee_reserve_warning, "0.010000000 NATIVE", "0.011000000 NATIVE"))
            .thenReturn("reserve warning")
    }

    @Test
    fun `reserve warning uses the same retained amount as maximum stake`() {
        assertEquals("reserve warning", formatter.feeReserveWarning("10", asset, fee))
        assertNull(formatter.feeReserveWarning("9.989", asset, fee))
        assertEquals("reserve warning", formatter.feeReserveWarning("9.989000001", asset, fee))
    }

    @Test
    fun `reserve warning does not invent fee or amount while either is absent`() {
        assertNull(formatter.feeReserveWarning("", asset, fee))
        assertNull(formatter.feeReserveWarning("0", asset, fee))
        assertNull(formatter.feeReserveWarning("10", asset, null))
    }

    @Test
    fun `an unknown fiat valuation stays absent without formatting an invented zero`() {
        val amount = formatter.amount(BigDecimal.ONE, TokenSymbol("CUSTOM"), null, currency)

        assertEquals("1 CUSTOM", amount.token)
        assertNull(amount.fiat)
        verifyNoInteractions(fiatFormatter)
    }
}
