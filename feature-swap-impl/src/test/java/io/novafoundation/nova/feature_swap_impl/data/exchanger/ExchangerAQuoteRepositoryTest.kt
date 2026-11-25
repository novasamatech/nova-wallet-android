package io.novafoundation.nova.feature_swap_impl.data.exchanger

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteArgs
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountConstraints
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteResult
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.test_shared.any
import java.io.IOException
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import kotlin.time.Duration.Companion.minutes

class ExchangerAQuoteRepositoryTest {

    private val gson = Gson()

    private val assetIn = asset("asset-hub", 1, 6)

    private val assetOut = asset("bittensor", 0, 9)

    private val source = mock(ExchangerAQuoteSource::class.java)

    private val amountConstraints = ExchangerAAmountConstraints()

    private val repository = ExchangerAQuoteRepository(source, amountConstraints)

    @Test
    fun `Exchanger A digit limits respect both asset and provider precision`() {
        assertEquals(6, amountConstraints.forAsset(assetIn).maxFractionDigits)
        assertEquals(8, amountConstraints.forAsset(assetOut).maxFractionDigits)
        assertEquals(15, amountConstraints.forAsset(assetOut).maxIntegerDigits)
    }

    @Test
    fun `quote maps amounts fees and identity without deducting provider fees again`() = runBlocking {
        val args = args()
        `when`(source.quote(any())).thenReturn(remote())

        val result = repository.quote(args) as ExchangerAQuoteResult.Quote

        assertSame(args, result.args)
        assertSame(assetIn, result.amountIn.chainAsset)
        assertSame(assetOut, result.amountOut.chainAsset)
        assertEquals(BigInteger("100000000"), result.amountIn.amount)
        assertEquals(BigInteger("42113900"), result.amountOut.amount)
        assertSame(assetIn, result.depositFee?.chainAsset)
        assertEquals(BigInteger("1700"), result.depositFee?.amount)
        assertSame(assetOut, result.withdrawalFee?.chainAsset)
        assertEquals(BigInteger("60000"), result.withdrawalFee?.amount)
        assertEquals(10.minutes..60.minutes, result.executionTime)
        assertEquals(BigInteger("1000000"), result.limits.min)
        assertNull(result.limits.max)
    }

    @Test
    fun `receive quote retains the backend estimate even when it differs from requested output`() = runBlocking {
        val args = args(amount = "0.04", direction = SwapDirection.SPECIFIED_OUT)
        var sent: ExchangerAQuoteRequest? = null
        `when`(source.quote(any())).thenAnswer {
            sent = it.getArgument(0)
            remote()
        }

        val result = repository.quote(args) as ExchangerAQuoteResult.Quote

        assertNull(sent?.payAmount)
        assertEquals("0.04", sent?.receiveAmount)
        assertEquals(BigInteger("40000000"), result.args.amount)
        assertEquals(BigInteger("42113900"), result.amountOut.amount)
    }

    @Test
    fun `request uses asset identifiers and exact plain token units`() {
        val request = mapExchangerAQuoteRequest(args(amount = "100.000001"), amountConstraints)

        assertEquals(assetIn.chainId, request.from.chainId)
        assertEquals(assetIn.id, request.from.assetId)
        assertEquals(assetOut.chainId, request.to.chainId)
        assertEquals("100.000001", request.payAmount)
        assertNull(request.receiveAmount)
    }

    @Test
    fun `eight decimal receive request is accepted without exponent notation`() {
        val request = mapExchangerAQuoteRequest(args("0.00000001", SwapDirection.SPECIFIED_OUT), amountConstraints)

        assertEquals("0.00000001", request.receiveAmount)
    }

    @Test
    fun `too precise nonpositive and overflowing amounts fail before requesting a quote`() = runBlocking {
        val invalid = listOf(
            args("0"),
            args("-1"),
            args("1000000000000000"),
            args("0.000000001", SwapDirection.SPECIFIED_OUT)
        )
        for (args in invalid) {
            assertTrue(runCatching { repository.quote(args) }.exceptionOrNull() is IllegalArgumentException)
        }
        verifyNoInteractions(source)
    }

    @Test
    fun `fifteen integer digits are accepted including amounts with stripped trailing zeros`() {
        for (amount in listOf("999999999999999.999999", "100000000000000")) {
            val request = mapExchangerAQuoteRequest(args(amount), amountConstraints)

            assertEquals(amount, request.payAmount)
        }
    }

    @Test
    fun `same asset request is rejected before HTTP`() = runBlocking {
        val args = args().let { it.copy(assetOut = it.assetIn) }

        assertTrue(runCatching { repository.quote(args) }.exceptionOrNull() is IllegalArgumentException)
        verifyNoInteractions(source)
    }

    @Test
    fun `null and zero fees remain distinct`() {
        val json = QUOTE.replace("\"0.0017\"", "null").replace("\"0.00006\"", "\"0\"")
            .replace("""{"min":10,"max":60}""", "null")
        val result = mapExchangerAQuote(remote(json), args()) as ExchangerAQuoteResult.Quote

        assertNull(result.depositFee)
        assertEquals(BigInteger.ZERO, result.withdrawalFee?.amount)
        assertNull(result.executionTime)
    }

    @Test
    fun `limit responses map both reasons with unbounded maximum allowed`() {
        val reasons = mapOf(
            "below_minimum" to ExchangerAQuoteResult.OutOfRange.Reason.BELOW_MINIMUM,
            "above_maximum" to ExchangerAQuoteResult.OutOfRange.Reason.ABOVE_MAXIMUM
        )
        for ((remoteReason, expectedReason) in reasons) {
            val json = """{"kind":"out_of_range","reason":"$remoteReason","limits":{"minPayAmount":"1","maxPayAmount":null}}"""
            val result = mapExchangerAQuote(remote(json), args()) as ExchangerAQuoteResult.OutOfRange
            assertEquals(expectedReason, result.reason)
            assertNull(result.limits.max)
        }
    }

    @Test
    fun `pay quote cannot silently change the requested payment`() {
        assertInvalid(QUOTE.replace("\"payAmount\":\"100\"", "\"payAmount\":\"101\""))
    }

    @Test
    fun `exact lower and upper payment limits are accepted`() {
        val json = QUOTE.replace("\"minPayAmount\":\"1\"", "\"minPayAmount\":\"100\"")
            .replace("\"maxPayAmount\":null", "\"maxPayAmount\":\"100\"")

        val result = mapExchangerAQuote(remote(json), args()) as ExchangerAQuoteResult.Quote

        assertEquals(result.amountIn.amount, result.limits.min)
        assertEquals(result.amountIn.amount, result.limits.max)
    }

    @Test
    fun `inverted limits and quoted payment outside limits are rejected`() {
        assertInvalid(
            QUOTE.replace("\"maxPayAmount\":null", "\"maxPayAmount\":\"0.5\""),
            QUOTE.replace("\"maxPayAmount\":null", "\"maxPayAmount\":\"99\""),
            QUOTE.replace("\"minPayAmount\":\"1\"", "\"minPayAmount\":\"101\"")
        )
    }

    @Test
    fun `malformed zero negative and over precise output amounts are rejected`() {
        for (invalid in listOf("0", "-1", "NaN", "1e3", "01", " 1", "0.0000000001")) {
            assertInvalid(QUOTE.replace("0.0421139", invalid))
        }
    }

    @Test
    fun `unknown response kind or limit reason cannot become a usable quote`() {
        assertInvalid(
            QUOTE.replace("\"kind\":\"quote\"", "\"kind\":\"future_kind\""),
            """{"kind":"out_of_range","reason":"future_reason","limits":{"minPayAmount":"1","maxPayAmount":null}}"""
        )
    }

    @Test
    fun `missing required response fields and partial duration are rejected`() {
        assertInvalid(
            "{}",
            QUOTE.replace("\"payAmount\":\"100\",", ""),
            QUOTE.replace("\"receiveAmount\":\"0.0421139\",", ""),
            QUOTE.replace("""{"minPayAmount":"1","maxPayAmount":null}""", "null"),
            QUOTE.replace("""{"deposit":"0.0017","withdrawal":"0.00006"}""", "null"),
            QUOTE.replace("""{"min":10,"max":60}""", """{"min":10}""")
        )
    }

    @Test
    fun `invalid fees and time ranges are rejected`() {
        assertInvalid(
            QUOTE.replace("0.0017", "-1"),
            QUOTE.replace("0.0017", "0.0000001"),
            QUOTE.replace("""{"min":10,"max":60}""", """{"min":-1,"max":60}"""),
            QUOTE.replace("""{"min":10,"max":60}""", """{"min":60,"max":10}""")
        )
    }

    @Test
    fun `transport errors and cancellation propagate from repository`() = runBlocking {
        for (expected in listOf(IOException("Unavailable"), CancellationException("Cancelled"))) {
            doAnswer { throw expected }.`when`(source).quote(any())

            assertSame(expected, runCatching { repository.quote(args()) }.exceptionOrNull())
        }
    }

    private fun assertInvalid(vararg responses: String) {
        for (json in responses) {
            val error = runCatching { mapExchangerAQuote(remote(json), args()) }.exceptionOrNull()
            assertTrue("Expected invalid quote error, got $error", error is IOException)
        }
    }

    private fun remote(json: String = QUOTE): ExchangerAQuoteRemote = gson.fromJson(json, ExchangerAQuoteRemote::class.java)

    private fun args(amount: String = "100", direction: SwapDirection = SwapDirection.SPECIFIED_IN): ExchangerAQuoteArgs {
        val specifiedAsset = if (direction == SwapDirection.SPECIFIED_IN) assetIn else assetOut
        return ExchangerAQuoteArgs(
            assetIn = assetIn,
            assetOut = assetOut,
            amount = specifiedAsset.planksFromAmount(amount.toBigDecimal()),
            swapDirection = direction
        )
    }

    private fun asset(chainId: String, id: Int, precision: Int) = Chain.Asset(
        icon = null,
        id = id,
        priceId = null,
        chainId = chainId,
        symbol = TokenSymbol(chainId),
        precision = precision.asPrecision(),
        buyProviders = emptyMap(),
        sellProviders = emptyMap(),
        staking = emptyList(),
        type = Chain.Asset.Type.Native,
        source = Chain.Asset.Source.DEFAULT,
        name = chainId
    )

    companion object {

        private val QUOTE = """
            {"kind":"quote","payAmount":"100","receiveAmount":"0.0421139",
             "limits":{"minPayAmount":"1","maxPayAmount":null},
             "networkFees":{"deposit":"0.0017","withdrawal":"0.00006"},
             "durationMinutes":{"min":10,"max":60},"warning":null}
        """.trimIndent()
    }
}
