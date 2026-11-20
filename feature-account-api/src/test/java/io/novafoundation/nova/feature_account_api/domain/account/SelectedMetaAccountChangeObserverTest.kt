package io.novafoundation.nova.feature_account_api.domain.account

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SelectedMetaAccountChangeObserverTest {

    private val accountRepository = mock(AccountRepository::class.java)

    private val selectedAccount = MutableStateFlow(account(id = 1))

    private val observer = SelectedMetaAccountChangeObserver(accountRepository)

    @Before
    fun setUp() {
        `when`(accountRepository.selectedMetaAccountFlow()).thenReturn(selectedAccount)
    }

    @Test
    fun `observe ignores initial account and metadata changes but reports switching back`() = runTest {
        val changes = mutableListOf<Long>()
        observer.observe(backgroundScope) { changes.add(it) }
        runCurrent()
        assertTrue(changes.isEmpty())

        selectedAccount.value = account(id = 1, name = "Renamed wallet")
        runCurrent()
        assertTrue(changes.isEmpty())

        selectedAccount.value = account(id = 2)
        runCurrent()
        selectedAccount.value = account(id = 2, name = "Another name")
        runCurrent()
        selectedAccount.value = account(id = 1)
        runCurrent()

        assertEquals(listOf(2L, 1L), changes)
    }

    @Test
    fun `observeOnce completes after leaving the expected account`() = runTest {
        val changes = mutableListOf<Long>()
        val subscription = observer.observeOnce(
            initialMetaAccountId = 1,
            scope = backgroundScope
        ) { changes.add(it) }
        runCurrent()
        selectedAccount.value = account(id = 1, name = "Renamed wallet")
        runCurrent()
        assertTrue(changes.isEmpty())

        selectedAccount.value = account(id = 2)
        runCurrent()
        assertTrue(subscription.isCompleted)

        selectedAccount.value = account(id = 3)
        runCurrent()
        assertEquals(listOf(2L), changes)
    }

    @Test
    fun `observeOnce reports an already changed account on subscription`() = runTest {
        val changes = mutableListOf<Long>()
        val subscription = observer.observeOnce(
            initialMetaAccountId = 2,
            scope = backgroundScope
        ) { changes.add(it) }
        runCurrent()

        assertEquals(listOf(1L), changes)
        assertTrue(subscription.isCompleted)
    }

    @Test
    fun `subscriptions have independent baselines and can be cancelled separately`() = runTest {
        val firstChanges = mutableListOf<Long>()
        val secondChanges = mutableListOf<Long>()
        val firstSubscription = observer.observe(backgroundScope) { firstChanges.add(it) }
        runCurrent()
        selectedAccount.value = account(id = 2)
        runCurrent()

        observer.observe(backgroundScope) { secondChanges.add(it) }
        runCurrent()
        assertTrue(secondChanges.isEmpty())

        selectedAccount.value = account(id = 3)
        runCurrent()
        firstSubscription.cancel()
        runCurrent()
        selectedAccount.value = account(id = 4)
        runCurrent()

        assertEquals(listOf(2L, 3L), firstChanges)
        assertEquals(listOf(3L, 4L), secondChanges)
    }

    @Test
    fun `cancelling scope cancels subscription and suspended callback`() = runTest {
        val scopeJob = Job(coroutineContext[Job])
        val observationScope = CoroutineScope(coroutineContext + scopeJob)
        val changes = mutableListOf<Long>()
        var callbackCancelled = false
        val subscription = observer.observe(observationScope) {
            changes.add(it)
            try {
                awaitCancellation()
            } finally {
                callbackCancelled = true
            }
        }
        runCurrent()
        selectedAccount.value = account(id = 2)
        runCurrent()
        assertEquals(listOf(2L), changes)

        observationScope.cancel()
        runCurrent()
        selectedAccount.value = account(id = 3)
        runCurrent()

        assertTrue(subscription.isCancelled)
        assertTrue(callbackCancelled)
        assertEquals(listOf(2L), changes)
    }

    @Test
    fun `observeOnce can be cancelled before account changes`() = runTest {
        val changes = mutableListOf<Long>()
        val subscription = observer.observeOnce(
            initialMetaAccountId = 1,
            scope = backgroundScope
        ) { changes.add(it) }
        runCurrent()

        subscription.cancel()
        runCurrent()
        selectedAccount.value = account(id = 2)
        runCurrent()

        assertTrue(changes.isEmpty())
        assertTrue(subscription.isCancelled)
    }

    private fun account(id: Long, name: String = "Wallet"): MetaAccount {
        val account = mock(MetaAccount::class.java)
        `when`(account.id).thenReturn(id)
        `when`(account.name).thenReturn(name)
        return account
    }
}
