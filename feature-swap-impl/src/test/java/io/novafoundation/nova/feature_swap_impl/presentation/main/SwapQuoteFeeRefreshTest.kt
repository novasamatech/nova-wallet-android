package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionEstimate
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapPoolId
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.matchesQuote
import io.novafoundation.nova.feature_swap_api.domain.model.quoteIdentity
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeConstructor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.mockito.Mockito.`when`
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SwapQuoteFeeRefreshTest {

    private val assetIn = asset(1)

    private val assetOut = asset(2)

    @Test
    fun `one second blocks allow four second fees and refresh quotes every ten seconds`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 4_000)
        fixture.start()
        runCurrent()
        fixture.continuousBlocks()
        runCurrent()
        val original = fixture.quotes.value

        advanceAndRun(4_300)
        assertSame(original, fixture.quotes.value)
        assertTrue(fixture.isReady())
        assertEquals(1, fixture.estimator.started.size)

        advanceAndRun(5_700)
        assertEquals(1, fixture.refreshes.size)
        assertFalse(fixture.isReady())
        val refreshed = fixture.quotes.value

        advanceAndRun(4_300)
        assertSame(refreshed, fixture.quotes.value)
        assertTrue(fixture.isReady())
        advanceAndRun(10_000)
        assertEquals(2, fixture.refreshes.size)
        assertTrue(fixture.isReady())
        assertEquals(0, fixture.estimator.cancelled)
    }

    @Test
    fun `fees slower than the refresh interval are never restarted by pending ticks`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 25_000)
        fixture.start()
        runCurrent()
        fixture.continuousBlocks()
        runCurrent()
        val original = fixture.quotes.value

        advanceAndRun(25_299)
        assertSame(original, fixture.quotes.value)
        assertTrue(fixture.refreshes.isEmpty())
        assertEquals(1, fixture.estimator.started.size)
        assertFalse(fixture.isReady())

        advanceAndRun(1)
        assertTrue(fixture.isReady())
        advanceAndRun(4_700)
        assertEquals(1, fixture.refreshes.size)
        advanceAndRun(25_300)
        assertTrue(fixture.isReady())
        assertEquals(0, fixture.estimator.cancelled)
    }

    @Test
    fun `completed quote cannot be replaced before fee loading notification or with stale fee settings`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 4_000)
        fixture.start(collectFees = false)
        runCurrent()
        fixture.blocks.emit(Unit)
        runCurrent()
        assertTrue(fixture.refreshes.isEmpty()) // Loaded quote + NoFee, quote job already finished.

        val current = fixture.quotes.value as QuotingState.Loaded
        val oldSettings = fixture.settings.value.copy(slippage = 2.percents)
        val oldFee = fee(current.quote, oldSettings.slippage)
        fixture.loader.fee.value = loadedFee(oldFee)
        advanceAndRun(10_000)
        fixture.blocks.emit(Unit)
        runCurrent()
        assertTrue(fixture.refreshes.isEmpty())

        fixture.loader.fee.value = loadedFee(fee(current.quote, fixture.settings.value.slippage))
        fixture.quoteActive = true
        advanceAndRun(10_000)
        fixture.blocks.emit(Unit)
        runCurrent()
        assertTrue(fixture.refreshes.isEmpty()) // Matching fee must not bypass an active quote job.

        fixture.quoteActive = false
        advanceAndRun(10_000)
        fixture.blocks.emit(Unit)
        runCurrent()
        assertEquals(1, fixture.refreshes.size)
    }

    @Test
    fun `amount asset direction and slippage edits immediately invalidate in flight fees`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 4_000)
        fixture.start()
        runCurrent()
        fixture.continuousBlocks()
        advanceAndRun(500)

        val initial = fixture.settings.value
        val changes = listOf(
            initial.copy(amount = 101.toBigInteger()),
            initial.copy(assetIn = assetOut, assetOut = assetIn),
            initial.copy(swapDirection = SwapDirection.SPECIFIED_OUT),
            initial.copy(slippage = 2.percents)
        )
        changes.forEach { settings ->
            fixture.userQuote(settings)
            runCurrent()
            assertTrue(fixture.loader.fee.value is FeeStatus.Loading)
            assertFalse(fixture.isReady())
            advanceAndRun(500)
        }

        advanceAndRun(3_800)
        assertTrue(fixture.isReady())
        assertEquals(4, fixture.estimator.cancelled)
        assertEquals(changes.last().slippage, fixture.estimator.started.last().slippage)
        assertTrue(fixture.refreshes.isEmpty())
    }

    @Test
    fun `quiet network fee error disables continue and retry keeps the current quote and terms`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 4_000)
        fixture.estimator.failNext = true
        fixture.start()
        runCurrent()
        val original = fixture.quotes.value

        advanceAndRun(4_300)
        assertSame(FeeStatus.Error, fixture.loader.fee.value)
        assertEquals(DescriptiveButtonState.Disabled("Continue"), fixture.buttonState())

        fixture.loader.retry()
        fixture.continuousBlocks()
        advanceAndRun(4_000)
        assertSame(original, fixture.quotes.value)
        assertTrue(fixture.isReady())
        assertEquals(fixture.estimator.started.first().quoteIdentity(), fixture.estimator.started.last().quoteIdentity())
        assertTrue(fixture.refreshes.isEmpty())
    }

    @Test
    fun `default and quote error allow first tick recovery while loading never cancels itself`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 4_000)
        fixture.quotes.value = QuotingState.Default
        fixture.start()
        runCurrent()
        fixture.blocks.emit(Unit)
        runCurrent()
        assertEquals(1, fixture.refreshes.size) // First tick is immediate, even on a quiet network.
        advanceAndRun(4_300)
        assertTrue(fixture.isReady())

        fixture.quotes.value = QuotingState.Error(IllegalStateException("offline"))
        runCurrent()
        assertSame(FeeStatus.NoFee, fixture.loader.fee.value)
        advanceAndRun(5_700)
        fixture.blocks.emit(Unit)
        runCurrent()
        assertEquals(2, fixture.refreshes.size)
        advanceAndRun(4_300)
        assertTrue(fixture.isReady())

        fixture.quotes.value = QuotingState.Loading
        fixture.quoteActive = true
        runCurrent()
        fixture.continuousBlocks()
        advanceAndRun(25_000)
        assertEquals(2, fixture.refreshes.size)
        assertSame(QuotingState.Loading, fixture.quotes.value)
    }

    @Test
    fun `loading without an active quote recovers on the next background tick`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 4_000)
        fixture.quotes.value = QuotingState.Loading
        fixture.quoteActive = true
        fixture.start()
        runCurrent()
        fixture.blocks.emit(Unit)
        runCurrent()
        assertTrue(fixture.refreshes.isEmpty())

        // A cancelled RPC request can finish its quote job while the screen itself remains active.
        fixture.quoteActive = false
        advanceAndRun(10_000)
        fixture.blocks.emit(Unit)
        runCurrent()
        assertEquals(1, fixture.refreshes.size)

        advanceAndRun(4_300)
        assertTrue(fixture.isReady())
        assertEquals(0, fixture.estimator.cancelled)
    }

    @Test
    fun `fee error permits background recovery but loading and absent mismatched fees do not`() = runTest {
        val fixture = fixture(backgroundScope, feeDuration = 4_000)
        fixture.start(collectFees = false)
        fixture.loader.fee.value = FeeStatus.Error
        runCurrent()
        fixture.blocks.emit(Unit)
        runCurrent()
        assertEquals(1, fixture.refreshes.size)

        fixture.loader.fee.value = FeeStatus.Loading(true)
        fixture.continuousBlocks()
        advanceAndRun(20_000)
        assertEquals(1, fixture.refreshes.size)
    }

    @Test
    fun `cancelling screen work cancels its estimate and pending refreshes`() = runTest {
        val lifetime = Job(backgroundScope.coroutineContext[Job])
        val scope = CoroutineScope(backgroundScope.coroutineContext + lifetime)
        val fixture = fixture(scope, feeDuration = 4_000)
        fixture.start()
        runCurrent()
        fixture.continuousBlocks()
        advanceAndRun(500)
        lifetime.cancel()
        runCurrent()
        val original = fixture.quotes.value

        advanceAndRun(30_000)
        assertSame(original, fixture.quotes.value)
        assertEquals(1, fixture.estimator.cancelled)
        assertTrue(fixture.refreshes.isEmpty())
    }

    @Test
    fun `readiness never enables an obsolete pair and terminal fee states do not spin`() {
        val button = DescriptiveButtonState.Enabled("Continue")
        val currentFee = loadedFee(fee(quote(defaultSettings()).quote, 1.percents))
        assertSame(DescriptiveButtonState.Loading, button.withSwapFeeReadiness(false, currentFee))
        assertSame(button, button.withSwapFeeReadiness(true, currentFee))
        assertEquals(DescriptiveButtonState.Disabled("Continue"), button.withSwapFeeReadiness(false, FeeStatus.Error))
        assertEquals(DescriptiveButtonState.Disabled("Continue"), button.withSwapFeeReadiness(false, FeeStatus.NoFee))
        assertEquals(DescriptiveButtonState.Disabled("Continue"), button.withSwapFeeReadiness(true, FeeStatus.Error))
        val invalidInput = DescriptiveButtonState.Disabled("Invalid amount")
        assertSame(invalidInput, invalidInput.withSwapFeeReadiness(true, currentFee))
    }

    private fun fixture(scope: CoroutineScope, feeDuration: Long): RefreshFixture {
        @Suppress("UNCHECKED_CAST")
        val unusedLoader = mock(FeeLoaderMixinV2.Presentation::class.java) as FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay>
        val loader = TestFeeLoader(scope, unusedLoader)
        val estimator = RecordingEstimator(feeDuration)
        return RefreshFixture(scope, loader, estimator)
    }

    private inner class RefreshFixture(
        private val scope: CoroutineScope,
        val loader: TestFeeLoader,
        val estimator: RecordingEstimator,
    ) {

        val settings = MutableStateFlow(defaultSettings())

        val quotes = MutableStateFlow<QuotingState>(quote(settings.value))

        val blocks = MutableSharedFlow<Unit>()

        val refreshes = mutableListOf<SwapSettings>()

        var quoteActive = false

        fun start(collectFees: Boolean = true) {
            if (collectFees) scope.launch { collectSwapFees(quotes, settings, loader, estimator::estimate) }
            blocks.backgroundQuoteRefreshes(
                settings = settings,
                quotingState = quotes,
                fee = loader.fee,
            ) { quoteActive }
                .onEach {
                    refreshes += it
                    quotes.value = quote(it, output = 200 + refreshes.size, origin = QuoteRefreshOrigin.SUBSCRIPTION)
                }
                .launchIn(scope)
        }

        fun continuousBlocks() = scope.launch {
            while (true) {
                blocks.emit(Unit)
                delay(1_000)
            }
        }

        fun userQuote(newSettings: SwapSettings) {
            settings.value = newSettings
            quotes.value = quote(newSettings)
        }

        fun isReady(): Boolean {
            val current = quotes.value as? QuotingState.Loaded ?: return false
            return current.matchesSettings(settings.value) &&
                loader.fee.value.loadedFeeOrNull()?.matchesQuote(current.quote, settings.value.slippage) == true
        }

        fun buttonState() = DescriptiveButtonState.Enabled("Continue").withSwapFeeReadiness(isReady(), loader.fee.value)
    }

    private class RecordingEstimator(private val duration: Long) {

        val started = mutableListOf<SwapFeeArgs>()

        var cancelled = 0

        var failNext = false

        suspend fun estimate(args: SwapFeeArgs): SwapFee {
            started += args
            try {
                delay(duration)
                if (failNext) {
                    failNext = false
                    error("offline")
                }
                val fee = mock(SwapFee::class.java)
                val identity = args.quoteIdentity()
                `when`(fee.quoteIdentity).thenReturn(identity)
                return fee
            } catch (error: CancellationException) {
                cancelled++
                throw error
            }
        }
    }

    // Models only FeeLoader's asynchronous request invalidation/retry contract, not its formatting or UI.
    private class TestFeeLoader(
        private val scope: CoroutineScope,
        unusedLoader: FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay>,
    ) : FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay> by unusedLoader {

        override val fee = MutableStateFlow<FeeStatus<SwapFee, FeeDisplay>>(FeeStatus.NoFee)

        private var job: Job? = null

        private var constructor: FeeConstructor<SwapFee>? = null

        override suspend fun setFeeLoading() {
            job?.cancel()
            constructor = null
            fee.value = FeeStatus.Loading(true)
        }

        override suspend fun setFeeStatus(feeStatus: FeeStatus<SwapFee, FeeDisplay>) {
            setFeeLoading()
            fee.value = feeStatus
        }

        override fun loadFee(feeConstructor: FeeConstructor<SwapFee>) {
            job?.cancel()
            constructor = feeConstructor
            fee.value = FeeStatus.Loading(true)
            job = scope.launch {
                try {
                    val result = feeConstructor(FeePaymentCurrency.Native)
                    currentCoroutineContext().ensureActive()
                    fee.value = if (result == null) FeeStatus.NoFee else loadedFee(result)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    fee.value = FeeStatus.Error
                }
            }
        }

        fun retry() = loadFee(checkNotNull(constructor))
    }

    private fun defaultSettings() = SwapSettings(assetIn, assetOut, 100.toBigInteger(), SwapDirection.SPECIFIED_IN, 1.percents)

    private fun quote(
        settings: SwapSettings,
        output: Int = 200,
        origin: QuoteRefreshOrigin = QuoteRefreshOrigin.USER_INPUT,
    ): QuotingState.Loaded {
        val inputAsset = settings.assetIn!!
        val outputAsset = settings.assetOut!!
        val direction = settings.swapDirection!!
        val amount = settings.amount!!
        val amountIn = if (direction == SwapDirection.SPECIFIED_IN) amount else 100.toBigInteger()
        val amountOut = if (direction == SwapDirection.SPECIFIED_OUT) amount else output.toBigInteger()
        val inputAssetId = inputAsset.fullId
        val outputAssetId = outputAsset.fullId
        val edge = mock(SwapGraphEdge::class.java)
        `when`(edge.from).thenReturn(inputAssetId)
        `when`(edge.to).thenReturn(outputAssetId)
        `when`(edge.poolId).thenReturn(SwapPoolId("chain", "pool"))
        val path = QuotedPath(direction, listOf(QuotedEdge(amountIn, amountOut, edge)), PathRoughFeeEstimation.zero())
        val quote = SwapQuote(
            ChainAssetWithAmount(inputAsset, amountIn),
            ChainAssetWithAmount(outputAsset, amountOut),
            Fraction.ZERO,
            path,
            SwapExecutionEstimate(emptyList(), Duration.ZERO),
            direction
        )
        val tokenIn = mock(Token::class.java)
        val tokenOut = mock(Token::class.java)
        `when`(tokenIn.configuration).thenReturn(inputAsset)
        `when`(tokenOut.configuration).thenReturn(outputAsset)
        val args = SwapQuoteArgs(tokenIn, tokenOut, amount, direction)
        return QuotingState.Loaded(quote, args, origin)
    }

    private fun fee(quote: SwapQuote, slippage: Fraction): SwapFee {
        val fee = mock(SwapFee::class.java)
        val args = quote.toExecuteArgs(slippage, FeePaymentCurrency.Native)
        val identity = args.quoteIdentity()
        `when`(fee.quoteIdentity).thenReturn(identity)
        return fee
    }

    private fun asset(id: Int): Chain.Asset {
        val asset = mock(Chain.Asset::class.java)
        `when`(asset.chainId).thenReturn("chain")
        `when`(asset.id).thenReturn(id)
        return asset
    }

    private fun TestScope.advanceAndRun(milliseconds: Long) {
        advanceTimeBy(milliseconds)
        runCurrent()
    }

    private companion object {

        fun loadedFee(fee: SwapFee): FeeStatus<SwapFee, FeeDisplay> = FeeStatus.Loaded(FeeModel(fee, FeeDisplay("fee", null)))
    }
}
