package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BittensorPortfolioHistoryRefreshTest {

    private val repository = mock(BittensorPortfolioHistoryRepository::class.java)

    private val calculator = BittensorPortfolioChartCalculator()

    private val accounts = mock(AccountRepository::class.java)

    private val chains = mock(ChainRegistry::class.java)

    private val updates = BittensorPositionUpdates()

    private val interactor = BittensorPortfolioChartInteractor(repository, calculator, accounts, chains, updates)

    private val chain = mock(Chain::class.java)

    private val account = mock(MetaAccount::class.java)

    private val currency = mock(Currency::class.java)

    private val request = BittensorPortfolioHistoryRequest(1L, "chain", null, currency, BittensorPortfolioPeriod.ONE_DAY)

    private val status = BittensorPortfolioHistoryStatus(BigDecimal.ZERO, false)

    private val historyAccount = BittensorAccountHistory(BittensorPortfolioPeriod.ONE_DAY, 0, 1000, emptyList(), status)

    private val history = BittensorPortfolioHistory(historyAccount, null)

    private suspend fun prepare() {
        `when`(chains.getChain("chain")).thenReturn(chain)
        `when`(accounts.getMetaAccount(1L)).thenReturn(account)
        `when`(account.accountIdIn(chain)).thenReturn(ByteArray(32))
        `when`(repository.load(org.mockito.ArgumentMatchers.eq(request) ?: request, anyString() ?: "")).thenReturn(history)
    }

    @Test
    fun `initial load periodic refresh and own confirmed operation reload history`() = runTest {
        prepare()
        var loads = 0
        val job = launch { interactor.observeHistory(request).collect { loads++ } }
        runCurrent()
        assertEquals(1, loads)
        advanceTimeBy(299_999)
        runCurrent()
        assertEquals(1, loads)
        updates.confirmed(1L, "chain", null)
        runCurrent()
        assertEquals(2, loads)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(3, loads)
        job.cancelAndJoin()
        updates.confirmed(1L, "chain", null)
        advanceTimeBy(300_000)
        runCurrent()
        assertEquals(3, loads)
    }

    @Test
    fun `another wallet or network cannot reload this account history`() = runTest {
        prepare()
        var loads = 0
        val job = launch { interactor.observeHistory(request).collect { loads++ } }
        runCurrent()
        updates.confirmed(2L, "chain", null)
        updates.confirmed(1L, "another-chain", null)
        runCurrent()
        assertEquals(1, loads)
        job.cancelAndJoin()
    }

    @Test
    fun `cancelling history observation is not converted to an empty success`() = runTest {
        prepare()
        `when`(repository.load(org.mockito.ArgumentMatchers.eq(request) ?: request, anyString() ?: ""))
            .thenThrow(CancellationException("Switched wallet"))
        var emitted = false
        val job = launch { interactor.observeHistory(request).collect { emitted = true } }
        runCurrent()
        assertTrue(job.isCancelled)
        assertEquals(false, emitted)
    }
}
