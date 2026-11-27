package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.feature_swap_impl.presentation.main.QuotingState.Loaded.PreviewOnly
import io.novafoundation.nova.feature_swap_impl.presentation.main.QuotingState.Loaded.ReadyForReview
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionEstimate
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.ExternalSwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.presentation.state.DEFAULT_SLIPPAGE
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigInteger
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SwapFeeRefreshTest {

    private val assetIn = mock(Chain.Asset::class.java)

    private val assetOut = mock(Chain.Asset::class.java)

    private val token = mock(Token::class.java)

    private val edge = mock(SwapGraphEdge::class.java)

    private val swapFee = mock(SwapFee::class.java)

    private val settings = SwapSettings(assetIn, assetOut, BigInteger.valueOf(100), SwapDirection.SPECIFIED_IN, DEFAULT_SLIPPAGE)

    private val quoteState = MutableStateFlow<QuotingState>(QuotingState.Default)

    private val startedAt = mutableListOf<Long>()

    private val completedFees = mutableListOf<SwapFeeSnapshot>()

    @Test
    fun `provider quote replaces pending on-chain fee estimation`() = runTest {
        startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(100)
        quoteState.value = PreviewOnly(mock(ExternalSwapQuote::class.java))
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(startedAt.isEmpty())
    }

    @Test
    fun `two second market updates allow three second fee to complete and refresh fees every ten seconds`() = runTest {
        startFeeLoading()
        repeat(12) { block ->
            quoteState.value = loadedQuote(output = 1_000L + block)
            runCurrent()
            advanceTimeBy(2_000)
        }
        runCurrent()

        assertEquals(listOf(300L, 10_300L, 20_300L), startedAt)
        assertEquals(3, completedFees.size)
        assertEquals(BigInteger.valueOf(1_010), completedFees.last().quote.quote.planksOut)
        assertEquals(BigInteger.valueOf(1_011), (quoteState.value as ReadyForReview).quote.planksOut)
        assertTrue(completedFees.last().matchesInputs(settings, metaAccountId = 1))
    }

    @Test
    fun `unchanged market quote still refreshes fee in the next window`() = runTest {
        startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(12_000)
        runCurrent()

        assertEquals(listOf(300L, 10_300L), startedAt)
        assertEquals(2, completedFees.size)
    }

    @Test
    fun `user input bypasses interval and resets the next background deadline`() = runTest {
        startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = loadedQuote(output = 2_000)
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = loadedQuote(output = 3_000, origin = QuoteRefreshOrigin.USER_INPUT)
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = loadedQuote(output = 4_000)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()

        assertEquals(listOf(300L, 4_300L, 14_300L), startedAt)
    }

    @Test
    fun `market quote arriving during input debounce cannot delay changed amount fee`() = runTest {
        startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(4_000)
        val changedSettings = settings.copy(amount = BigInteger.valueOf(200))
        quoteState.value = loadedQuote(origin = QuoteRefreshOrigin.USER_INPUT, quoteSettings = changedSettings)
        runCurrent()
        advanceTimeBy(100)
        quoteState.value = loadedQuote(output = 2_000, quoteSettings = changedSettings)
        runCurrent()
        advanceTimeBy(301)

        assertEquals(listOf(300L, 4_400L), startedAt)
    }

    @Test
    fun `changed inputs bypass interval even when state flow conflates user quote`() = runTest {
        startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(4_000)
        val changedSettings = settings.copy(slippage = 1.percents)
        quoteState.value = loadedQuote(origin = QuoteRefreshOrigin.USER_INPUT, quoteSettings = changedSettings)
        quoteState.value = loadedQuote(output = 2_000, quoteSettings = changedSettings)
        runCurrent()
        advanceTimeBy(301)

        assertEquals(listOf(300L, 4_300L), startedAt)
    }

    @Test
    fun `loading cancels queued background fee while new user quote is pending`() = runTest {
        startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = loadedQuote(output = 2_000)
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = QuotingState.Loading
        runCurrent()
        advanceTimeBy(8_000)
        runCurrent()
        assertEquals(listOf(300L), startedAt)

        quoteState.value = loadedQuote(origin = QuoteRefreshOrigin.USER_INPUT)
        runCurrent()
        advanceTimeBy(301)
        assertEquals(listOf(300L, 12_300L), startedAt)
    }

    @Test
    fun `clearing form cancels queued fee refresh`() = runTest {
        startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = loadedQuote(output = 2_000)
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = QuotingState.Default
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(listOf(300L), startedAt)
    }

    @Test
    fun `leaving screen cancels queued fee refresh`() = runTest {
        val job = startFeeLoading()
        quoteState.value = loadedQuote()
        runCurrent()
        advanceTimeBy(2_000)
        quoteState.value = loadedQuote(output = 2_000)
        runCurrent()
        job.cancel()
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(listOf(300L), startedAt)
    }

    @Test
    fun `fee is invalid after changing amount tokens direction slippage or wallet`() {
        val snapshot = SwapFeeSnapshot(loadedQuote(), swapFee)
        val changedSettings = listOf(
            settings.copy(amount = BigInteger.valueOf(200)),
            settings.copy(assetIn = assetOut),
            settings.copy(assetOut = assetIn),
            settings.copy(swapDirection = SwapDirection.SPECIFIED_OUT),
            settings.copy(slippage = 1.percents),
        )

        changedSettings.forEach { assertFalse(snapshot.matchesInputs(it, metaAccountId = 1)) }
        assertFalse(snapshot.matchesInputs(settings, metaAccountId = 2))
    }

    @Test
    fun `confirmation preserves the quote used for fee estimation after market updates`() {
        val estimatedQuote = loadedQuote()
        val snapshot = SwapFeeSnapshot(estimatedQuote, swapFee)
        quoteState.value = loadedQuote(output = 2_000)

        val payload = snapshot.toValidationPayload()

        assertSame(estimatedQuote.quote, payload.swapQuote)
        assertSame(swapFee, payload.fee)
        assertEquals(settings.slippage, payload.slippage)
        assertEquals(1L, payload.metaAccountId)
    }

    private fun TestScope.startFeeLoading(): Job {
        var feeJob: Job? = null
        return quoteState.throttleFeeRefreshes(testScheduler.timeSource)
            .onEach { quote ->
                startedAt += testScheduler.currentTime
                feeJob?.cancel()
                feeJob = backgroundScope.launch {
                    delay(3_000)
                    val snapshot = SwapFeeSnapshot(quote, swapFee)
                    if (snapshot.matchesInputs(settings, metaAccountId = 1)) completedFees += snapshot
                }
            }
            .launchIn(backgroundScope)
    }

    private fun loadedQuote(
        output: Long = 1_000,
        origin: QuoteRefreshOrigin = QuoteRefreshOrigin.SUBSCRIPTION,
        quoteSettings: SwapSettings = settings,
    ): ReadyForReview {
        val amountIn = requireNotNull(quoteSettings.amount)
        val amountOut = BigInteger.valueOf(output)
        val direction = requireNotNull(quoteSettings.swapDirection)
        val path = QuotedPath(direction, listOf(QuotedEdge(amountIn, amountOut, edge)), PathRoughFeeEstimation.zero(), netAmountOut = amountOut)
        val quote = SwapQuote(
            amountIn = ChainAssetWithAmount(assetIn, amountIn),
            amountOut = ChainAssetWithAmount(assetOut, amountOut),
            priceImpact = Fraction.ZERO,
            quotedPath = path,
            executionEstimate = SwapExecutionEstimate(emptyList(), Duration.ZERO),
            direction = direction,
        )
        val args = SwapQuoteArgs(token, token, amountIn, direction, metaAccountId = 1)
        return ReadyForReview(quote, args, origin, quoteSettings)
    }
}
