package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorRequiredAmountFormattingTest {

    private val configs = mutableListOf<TokenConfig>()

    // Mirrors the real formatter's abbreviated path, which honours the configured rounding mode.
    private val formatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence {
            configs += config
            return amount.formatTokenAmount(config.roundingMode)
        }
    }

    private val token = mock(Token::class.java).also { token ->
        val chainAsset = mock(Chain.Asset::class.java)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("TAO"))
        `when`(token.configuration).thenReturn(chainAsset)
    }

    @Test
    fun `required amount is rounded up so the shown value passes the minimum`() {
        val required = BigInteger("2017151")
        val shown = formatter.formatBittensorRequiredAmount(required, token).toString()

        assertEquals(RoundingMode.CEILING, configs.single().roundingMode)
        assertTrue(BigDecimal(shown) >= BigDecimal(required, 9))
    }
}
