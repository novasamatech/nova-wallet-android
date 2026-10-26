package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorHistoryRequest
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorHistoryState
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistoryRepository
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorCostBasisInteractorTest {

    private val history = mock(BittensorOperationHistoryRepository::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val logger = mock(Logger::class.java)

    private val chain = mock(Chain::class.java)

    private val account = mock(MetaAccount::class.java)

    private val coldKey = ByteArray(32) { 1 }

    private val request = BittensorHistoryRequest("address", "https://infra.test/".toHttpUrl(), 0)

    private val interactor = BittensorCostBasisInteractor(history, accounts, logger)

    private val context = BittensorEstimateContext(7L, chain)

    private val ledger = BittensorOperationHistory(emptyList(), allProviderPagesRead = false)

    @Before
    fun setup() = runTest {
        `when`(chain.id).thenReturn("chain")
        `when`(account.accountIdIn(chain)).thenReturn(coldKey)
        `when`(accounts.metaAccountFlow(7L)).thenReturn(flowOf(account))
        `when`(accounts.getMetaAccount(7L)).thenReturn(account)
        `when`(history.request(chain.addressOf(coldKey))).thenReturn(request)
    }

    @Test
    fun `no context is unavailable rather than loading and reads no wallet`() = runTest {
        assertEquals(BittensorCostBasisState.Unavailable, interactor.observe(flowOf(null)).first())
        verifyNoInteractions(accounts, history)
    }

    @Test
    fun `history loading invalidation and failure are distinct from absent context`() = runTest {
        val updates = MutableStateFlow<Result<BittensorOperationHistory>?>(null)
        val historyStates = updates.map { BittensorHistoryState(it, false) }
        `when`(history.observeState(chain.addressOf(coldKey))).thenReturn(historyStates)
        val contexts = MutableStateFlow<BittensorEstimateContext?>(context)
        val states = mutableListOf<BittensorCostBasisState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            interactor.observe(contexts).toList(states)
        }
        assertEquals(BittensorCostBasisState.Loading, states.last())
        updates.value = Result.success(ledger)
        assertTrue(states.last() is BittensorCostBasisState.Loaded)
        updates.value = null
        assertEquals(BittensorCostBasisState.Loading, states.last())
        updates.value = Result.failure(IllegalStateException("offline"))
        assertTrue(states.last() is BittensorCostBasisState.Failed)
        contexts.value = null
        assertEquals(BittensorCostBasisState.Unavailable, states.last())
    }

    @Test
    fun `position updates retain settlement on loading and successful history`() = runTest {
        val historyStates = flowOf(
            BittensorHistoryState(null, true),
            BittensorHistoryState(Result.success(ledger), true),
            BittensorHistoryState(Result.success(ledger), false)
        )
        `when`(history.observeState(chain.addressOf(coldKey))).thenReturn(historyStates)
        val contexts = flowOf(context)

        val states = interactor.observeUpdates(contexts).toList()

        assertEquals(listOf(false, true, true, false), states.map { it.isSettling })
        assertEquals(BittensorCostBasisState.Loading, states[1].state)
        assertTrue(states[2].state is BittensorCostBasisState.Loaded)
        assertTrue(states[3].state is BittensorCostBasisState.Loaded)
    }

    @Test
    fun `same account emissions never restart history independent of passive stake changes`() = runTest {
        val accountUpdates = MutableStateFlow(account)
        val updates = MutableStateFlow<Result<BittensorOperationHistory>?>(Result.success(ledger))
        `when`(accounts.metaAccountFlow(7L)).thenReturn(accountUpdates)
        val historyStates = updates.map { BittensorHistoryState(it, false) }
        `when`(history.observeState(chain.addressOf(coldKey))).thenReturn(historyStates)
        val states = mutableListOf<BittensorCostBasisState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            interactor.observe(flowOf(context)).toList(states)
        }
        val count = states.size
        repeat(30) {
            val sameColdKeyAccount = mock(MetaAccount::class.java)
            `when`(sameColdKeyAccount.accountIdIn(chain)).thenReturn(coldKey)
            accountUpdates.value = sameColdKeyAccount
        }
        assertEquals(count, states.size)
        verify(history, times(1)).observeState(chain.addressOf(coldKey))
        // No PositionRepository dependency exists: stake amounts cannot clear/cancel this source.
    }

    @Test
    fun `same wallet with changed coldkey immediately switches to new account loading`() = runTest {
        val secondAccount = mock(MetaAccount::class.java)
        val secondKey = ByteArray(32) { 2 }
        `when`(secondAccount.accountIdIn(chain)).thenReturn(secondKey)
        val wallets = MutableStateFlow(account)
        val first = MutableStateFlow<Result<BittensorOperationHistory>?>(Result.success(ledger))
        val second = MutableStateFlow<Result<BittensorOperationHistory>?>(null)
        `when`(accounts.metaAccountFlow(7L)).thenReturn(wallets)
        val firstStates = first.map { BittensorHistoryState(it, false) }
        val secondStates = second.map { BittensorHistoryState(it, false) }
        `when`(history.observeState(chain.addressOf(coldKey))).thenReturn(firstStates)
        `when`(history.observeState(chain.addressOf(secondKey))).thenReturn(secondStates)
        val states = mutableListOf<BittensorCostBasisState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            interactor.observe(flowOf(context)).toList(states)
        }
        wallets.value = secondAccount
        assertEquals(BittensorCostBasisState.Loading, states.last())
        first.value = Result.failure(IllegalStateException("late old account"))
        assertEquals(BittensorCostBasisState.Loading, states.last())
    }

    @Test
    fun `success unknown and unconfirmed failure invalidate but atomic failures do not`() {
        interactor.onTradeResult(request, BittensorOperationResult.Success("hash"))
        interactor.onTradeResult(request, BittensorOperationResult.Unknown(null))
        interactor.onTradeResult(request, BittensorOperationResult.Failure(IllegalStateException("unknown")))
        interactor.onTradeResult(request, BittensorOperationResult.Failure(IllegalStateException("rejected"), stage = BittensorFailureStage.NOT_SUBMITTED))
        interactor.onTradeResult(request, BittensorOperationResult.Failure(IllegalStateException("failed"), stage = BittensorFailureStage.DISPATCHED))
        interactor.onTradeResult(null, BittensorOperationResult.Success("hash"))
        verify(history, times(3)).ownTradeSubmitted(request)
    }

    @Test
    fun `optional capture uses explicit wallet and does not block on missing or failed origin`() = runTest {
        assertEquals(request, interactor.ownTradeRequest(7L, chain))
        `when`(history.request(chain.addressOf(coldKey))).thenReturn(null)
        assertNull(interactor.ownTradeRequest(7L, chain))
        `when`(history.request(chain.addressOf(coldKey))).thenThrow(IllegalStateException("origin unavailable"))
        assertNull(interactor.ownTradeRequest(7L, chain))
    }

    @Test
    fun `optional capture preserves caller cancellation`() = runTest {
        `when`(history.request(chain.addressOf(coldKey))).thenThrow(CancellationException("cancel"))
        assertTrue(runCatching { interactor.ownTradeRequest(7L, chain) }.exceptionOrNull() is CancellationException)
    }
}
