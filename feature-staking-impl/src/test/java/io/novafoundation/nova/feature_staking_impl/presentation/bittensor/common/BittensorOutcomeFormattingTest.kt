package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorOutcomeFormattingTest {

    private val chain = mock(Chain::class.java)

    private val asset = mock(Chain.Asset::class.java)

    private val fee = mock(Fee::class.java)

    private val resources = mock(ResourceManager::class.java)

    private val formatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence {
            return "${amount.toPlainString()} ${token.value}"
        }
    }

    private val coldkey = AccountIdKey(ByteArray(32) { 1 })

    private val outcome = BittensorOperationOutcome(
        "block", BigInteger.TEN, null, BigInteger.ONE, BigInteger.TEN, coldkey, null, BittensorNetuid.Root, coldkey
    )

    @Before
    fun setup() {
        `when`(asset.id).thenReturn(0)
        `when`(asset.precision).thenReturn(Precision(2))
        `when`(asset.symbol).thenReturn(TokenSymbol("NATIVE"))
        `when`(chain.assets).thenReturn(listOf(asset))
        `when`(fee.asset).thenReturn(asset)
        `when`(fee.amount).thenReturn(BigInteger.valueOf(50))
    }

    @Test
    fun `actual amounts replace estimates without reusing estimated fiat`() {
        val actual = formatBittensorNativeResult(BigInteger.TEN, BigInteger.valueOf(50), chain, formatter)
        val estimate = formatBittensorNativeResult(null, BigInteger.valueOf(50), chain, formatter)
        assertEquals("0.10 NATIVE", actual.token)
        assertEquals("≈ 0.50 NATIVE", estimate.token)
        assertNull(actual.fiat)
        assertNull(estimate.fiat)
    }

    @Test
    fun `actual zero remains distinct from missing fee`() {
        assertEquals("0.00 NATIVE", outcome.copy(networkFee = BigInteger.ZERO).formatNetworkFee(fee, chain, resources, formatter).amount)
        assertEquals("≈ 0.50 NATIVE", outcome.copy(networkFee = null).formatNetworkFee(fee, chain, resources, formatter).amount)
    }

    @Test
    fun `unknown payer stays estimated but alpha fee native equivalent is actual`() {
        assertEquals("≈ 0.50 NATIVE", outcome.copy(feePayer = null).formatNetworkFee(fee, chain, resources, formatter).amount)
        assertEquals("0.10 NATIVE", outcome.copy(alphaFee = BigInteger.ONE).formatNetworkFee(fee, chain, resources, formatter).amount)
    }
}
