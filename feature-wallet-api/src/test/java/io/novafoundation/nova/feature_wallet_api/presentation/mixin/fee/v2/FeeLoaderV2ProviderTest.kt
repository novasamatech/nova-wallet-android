package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.Observer
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.runtime.ext.fullId

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.fee.FeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.ChooseFeeCurrencyPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.PaymentCurrencySelectionMode
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class FeeLoaderV2ProviderTest {

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val actionFactory = mock(ActionAwaitableMixin.Factory::class.java)

    private val chooseFee = mock(ActionAwaitableMixin.Presentation::class.java) as
        ActionAwaitableMixin.Presentation<ChooseFeeCurrencyPayload, Chain.Asset>

    private val resourceManager = mock(ResourceManager::class.java)

    private val interactor = EmptyFeeInteractor()

    private val nativeAsset = mock(Chain.Asset::class.java)

    private val obsoleteFeeAsset = mock(Chain.Asset::class.java)

    private val inspectedFeeAmount = FeeInspector.InspectedFeeAmount(BigInteger.ZERO, BigInteger.ZERO)

    private val feeInspector = object : FeeInspector<String> {
        override fun getSubmissionFeeAsset(fee: String) = if (fee == "old") obsoleteFeeAsset else nativeAsset

        override fun inspectFeeAmount(fee: String) = inspectedFeeAmount
    }

    @Test
    fun `new request invalidates loaded fee before dispatch`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        subject.setFee("old")
        assertEquals("old", subject.fee.value.loadedFeeOrNull())

        subject.loadFee { "new" }
        assertTrue(subject.fee.value is FeeStatus.Loading)
        runCurrent()
        assertEquals("new", subject.fee.value.loadedFeeOrNull())
    }

    @Test
    fun `setFeeLoading cancels the active constructor`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        val cancelled = CompletableDeferred<Unit>()
        subject.loadFee {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        runCurrent()

        subject.setFeeLoading()
        runCurrent()
        assertTrue(cancelled.isCompleted)
        assertTrue(subject.fee.value is FeeStatus.Loading)
    }

    @Test
    fun `cancellation ignoring old constructor cannot replace the new fee`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        val releaseOld = CompletableDeferred<Unit>()
        subject.loadFee {
            withContext(NonCancellable) {
                releaseOld.await()
                "old"
            }
        }
        runCurrent()
        subject.loadFee { "new" }
        runCurrent()

        releaseOld.complete(Unit)
        runCurrent()
        assertEquals("new", subject.fee.value.loadedFeeOrNull())
        assertEquals(listOf("new"), formatter.formattedFees)
    }

    @Test
    fun `late formatter cannot publish fee or currency after invalidation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val releaseOld = CompletableDeferred<Unit>()
        val formatter = TestFeeFormatter { fee ->
            if (fee == "old") withContext(NonCancellable) { releaseOld.await() }
        }
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        subject.loadFee { "old" }
        runCurrent()
        subject.setFeeLoading()

        releaseOld.complete(Unit)
        runCurrent()
        assertTrue(subject.fee.value is FeeStatus.Loading)
        assertSame(nativeAsset, subject.feeChainAssetFlow.replayCache.last())
        assertNull(subject.retryEvent.value)
    }

    @Test
    fun `obsolete error cannot overwrite current fee or post retry`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        val releaseOld = CompletableDeferred<Unit>()
        subject.loadFee {
            withContext(NonCancellable) {
                releaseOld.await()
                error("old request failed")
            }
        }
        runCurrent()
        subject.loadFee { "new" }
        runCurrent()
        releaseOld.complete(Unit)
        runCurrent()

        assertEquals("new", subject.fee.value.loadedFeeOrNull())
        assertNull(subject.retryEvent.value)
    }

    @Test
    fun `late no fee cannot hide current fee`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        val releaseOld = CompletableDeferred<Unit>()
        subject.loadFee {
            withContext(NonCancellable) {
                releaseOld.await()
                null
            }
        }
        runCurrent()
        subject.loadFee { "new" }
        runCurrent()
        releaseOld.complete(Unit)
        runCurrent()

        assertEquals("new", subject.fee.value.loadedFeeOrNull())
    }

    @Test
    fun `explicit status wins over a running estimate`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        val releaseOld = CompletableDeferred<Unit>()
        subject.loadFee {
            withContext(NonCancellable) {
                releaseOld.await()
                "old"
            }
        }
        runCurrent()
        subject.setFeeStatus(FeeStatus.NoFee)
        releaseOld.complete(Unit)
        runCurrent()

        assertTrue(subject.fee.value is FeeStatus.NoFee)
    }

    @Test
    fun `obsolete automatic balance check cannot change currency or restart old constructor`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val balanceAsset = mock(Asset::class.java)
        val balanceChecked = CompletableDeferred<Unit>()
        val releaseBalanceCheck = CompletableDeferred<Unit>()
        val balanceInteractor = SuspendedBalanceFeeInteractor(balanceAsset, balanceChecked, releaseBalanceCheck)
        val context = flowOf(FeeContext(obsoleteFeeAsset, FeeContext.OperationUtilityAssetSource.Specified(nativeAsset)))
        val subject = createSubject(
            scope = backgroundScope,
            dispatcher = dispatcher,
            formatter = formatter,
            feeInteractor = balanceInteractor,
            context = context,
            mode = PaymentCurrencySelectionMode.AUTOMATIC_ONLY
        )
        var estimates = 0
        subject.loadFee {
            estimates++
            "pending"
        }
        balanceChecked.await()
        subject.setFeeLoading()
        releaseBalanceCheck.complete(Unit)
        runCurrent()

        assertEquals(1, estimates)
        assertSame(nativeAsset, subject.feeChainAssetFlow.replayCache.last())
        assertTrue(subject.fee.value is FeeStatus.Loading)
    }

    @Test
    fun `detected custom fee formats through actual context token while native asset is absent`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val customToken = mock(Token::class.java)
        val customHolding = mock(Asset::class.java)
        `when`(customToken.configuration).thenReturn(obsoleteFeeAsset)
        `when`(customHolding.token).thenReturn(customToken)
        val tokenInteractor = CustomTokenFeeInteractor(obsoleteFeeAsset, customHolding)
        val formatter = TokenReadingFeeFormatter(obsoleteFeeAsset)
        val subject = createSubject(backgroundScope, dispatcher, formatter, feeInteractor = tokenInteractor)

        subject.loadFee { "old" } // Its actual submission asset is the custom asset, initially unselected.
        runCurrent()

        assertEquals("old", subject.fee.value.loadedFeeOrNull())
        assertSame(customToken, formatter.formattedToken)
        assertEquals(listOf(obsoleteFeeAsset), tokenInteractor.tokenRequests)
        assertSame(obsoleteFeeAsset, subject.feeChainAssetFlow.replayCache.last())
    }

    @Test
    fun `held UI queue cannot deliver retry from an invalidated fee generation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        val uiQueue = HeldUiTaskExecutor()
        val delivered = mutableListOf<RetryPayload>()
        val observer = Observer<Event<RetryPayload>> { event -> event?.getContentIfNotHandled()?.let(delivered::add) }
        val androidLog = mockStatic(Log::class.java)
        ArchTaskExecutor.getInstance().setDelegate(uiQueue)
        try {
            subject.retryEvent.observeForever(observer)
            subject.loadFee { error("old request failed") }
            runCurrent()
            assertTrue(subject.fee.value is FeeStatus.Error)
            assertEquals(1, uiQueue.queuedTasks)

            subject.loadFee { "new" }
            runCurrent()
            assertEquals("new", subject.fee.value.loadedFeeOrNull())
            uiQueue.deliverAll()
            assertTrue(delivered.isEmpty())

            // A current failure must still reach the same observer.
            subject.loadFee { error("current request failed") }
            runCurrent()
            uiQueue.deliverAll()
            assertEquals(1, delivered.size)
        } finally {
            subject.retryEvent.removeObserver(observer)
            ArchTaskExecutor.getInstance().setDelegate(null)
            androidLog.close()
        }
    }

    @Test
    fun `inactive observer cannot consume a retained retry after invalidation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val formatter = TestFeeFormatter()
        val subject = createSubject(backgroundScope, dispatcher, formatter)
        val uiQueue = HeldUiTaskExecutor()
        val delivered = mutableListOf<RetryPayload>()
        val observer = Observer<Event<RetryPayload>> { event -> event?.getContentIfNotHandled()?.let(delivered::add) }
        val androidLog = mockStatic(Log::class.java)
        ArchTaskExecutor.getInstance().setDelegate(uiQueue)
        try {
            subject.loadFee { error("old request failed") }
            runCurrent()
            uiQueue.deliverAll()
            val retainedEvent = subject.retryEvent.value!!

            subject.setFeeLoading()
            subject.retryEvent.observeForever(observer)
            assertTrue(retainedEvent.hasBeenHandled)
            assertTrue(delivered.isEmpty())
            uiQueue.deliverAll()
            assertNull(subject.retryEvent.value)
        } finally {
            subject.retryEvent.removeObserver(observer)
            ArchTaskExecutor.getInstance().setDelegate(null)
            androidLog.close()
        }
    }

    private fun createSubject(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        formatter: FeeFormatter<String, String>,
        feeInteractor: FeeInteractor = interactor,
        context: Flow<FeeContext> = emptyFlow(),
        mode: PaymentCurrencySelectionMode = PaymentCurrencySelectionMode.DETECT_FROM_FEE
    ): FeeLoaderV2Provider<String, String> {
        `when`(actionFactory.create<ChooseFeeCurrencyPayload, Chain.Asset>()).thenReturn(chooseFee)
        `when`(resourceManager.getString(R.string.choose_amount_network_error)).thenReturn("Network error")
        `when`(resourceManager.getString(R.string.choose_amount_error_fee)).thenReturn("Retry fee loading")
        `when`(nativeAsset.id).thenReturn(0)
        `when`(nativeAsset.chainId).thenReturn("chain")
        `when`(obsoleteFeeAsset.id).thenReturn(1)
        `when`(obsoleteFeeAsset.chainId).thenReturn("chain")
        val configuration = FeeLoaderMixinV2.Configuration<String, String>(
            initialState = FeeLoaderMixinV2.Configuration.InitialState(
                paymentCurrencySelectionMode = mode
            )
        )
        // Constructors and formatting run on the test scheduler; external checks use explicit completion gates.
        val subject = FeeLoaderV2Provider(
            chainRegistry = chainRegistry,
            actionAwaitableMixinFactory = actionFactory,
            resourceManager = resourceManager,
            interactor = feeInteractor,
            feeFormatter = formatter,
            configuration = configuration,
            feeInspector = feeInspector,
            feeContextFlow = context,
            coroutineScope = scope,
            feeLoadingDispatcher = dispatcher
        )
        subject.feeChainAssetFlow.tryEmit(nativeAsset)
        return subject
    }

    private class HeldUiTaskExecutor : TaskExecutor() {

        private val queue = java.util.ArrayDeque<Runnable>()

        val queuedTasks: Int
            get() = queue.size

        override fun executeOnDiskIO(runnable: Runnable) = runnable.run()

        override fun postToMainThread(runnable: Runnable) {
            queue.addLast(runnable)
        }

        override fun isMainThread() = true

        fun deliverAll() {
            while (queue.isNotEmpty()) queue.removeFirst().run()
        }
    }

    private class CustomTokenFeeInteractor(
        private val customAsset: Chain.Asset,
        private val customHolding: Asset
    ) : FeeInteractor {

        val tokenRequests = mutableListOf<Chain.Asset>()

        override suspend fun canPayFeeInAsset(chainAsset: Chain.Asset) = false

        override suspend fun assetFlow(asset: Chain.Asset): Flow<Asset?> {
            return flowOf(customHolding.takeIf { asset.fullId == customAsset.fullId })
        }

        override suspend fun hasEnoughBalanceToPayFee(feeAsset: Asset, inspectedFeeAmount: FeeInspector.InspectedFeeAmount) = false

        override suspend fun getToken(chainAsset: Chain.Asset): Token {
            check(chainAsset.fullId == customAsset.fullId)
            tokenRequests += chainAsset
            return customHolding.token
        }
    }

    private class TokenReadingFeeFormatter(private val customAsset: Chain.Asset) : FeeFormatter<String, String> {

        var formattedToken: Token? = null

        override suspend fun createLoadingStatus() = FeeStatus.Loading(visibleDuringProgress = true)

        override suspend fun formatFee(fee: String, configuration: FeeFormatter.Configuration, context: FeeFormatter.Context): String {
            formattedToken = context.token(customAsset)
            return fee
        }
    }

    private class EmptyFeeInteractor : FeeInteractor {

        override suspend fun canPayFeeInAsset(chainAsset: Chain.Asset) = false

        override suspend fun assetFlow(asset: Chain.Asset): Flow<Asset?> = flowOf(null)

        override suspend fun hasEnoughBalanceToPayFee(feeAsset: Asset, inspectedFeeAmount: FeeInspector.InspectedFeeAmount) = false

        override suspend fun getToken(chainAsset: Chain.Asset): Token = error("Formatter does not fetch tokens")
    }

    private class SuspendedBalanceFeeInteractor(
        private val balanceAsset: Asset,
        private val balanceChecked: CompletableDeferred<Unit>,
        private val releaseBalanceCheck: CompletableDeferred<Unit>
    ) : FeeInteractor {

        override suspend fun canPayFeeInAsset(chainAsset: Chain.Asset) = true

        override suspend fun assetFlow(asset: Chain.Asset) = flowOf(balanceAsset)

        override suspend fun hasEnoughBalanceToPayFee(feeAsset: Asset, inspectedFeeAmount: FeeInspector.InspectedFeeAmount): Boolean {
            return withContext(NonCancellable) {
                balanceChecked.complete(Unit)
                releaseBalanceCheck.await()
                false
            }
        }

        override suspend fun getToken(chainAsset: Chain.Asset): Token = error("Formatter does not fetch tokens")
    }

    private class TestFeeFormatter(
        private val beforeFormat: suspend (String) -> Unit = {}
    ) : FeeFormatter<String, String> {

        val formattedFees = mutableListOf<String>()

        override suspend fun createLoadingStatus() = FeeStatus.Loading(visibleDuringProgress = true)

        override suspend fun formatFee(fee: String, configuration: FeeFormatter.Configuration, context: FeeFormatter.Context): String {
            beforeFormat(fee)
            formattedFees += fee
            return fee
        }
    }
}
