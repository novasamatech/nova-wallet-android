package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorCostBasisInteractorTest {

    private val historyRepository = mock(BittensorOperationHistoryRepository::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val account = mock(MetaAccount::class.java)

    private val firstAsset = mock(Asset::class.java)

    private val changedAsset = mock(Asset::class.java)

    private val interactor = BittensorCostBasisInteractor(historyRepository, accounts, wallets)

    private val context = BittensorEstimateContext(7L, chain)

    @Before
    fun setup() {
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(firstAsset.transferableInPlanks).thenReturn(BigInteger.ONE)
        `when`(changedAsset.transferableInPlanks).thenReturn(BigInteger.TEN)
    }

    @Test
    fun `absent context clears estimates without reading a wallet`() = runTest {
        assertNull(interactor.observeRequests(flowOf(null)).first())
        verifyNoInteractions(wallets, accounts, historyRepository)
    }

    @Test
    fun `balance changes create new requests and clearing the context unsubscribes`() = runTest {
        val balances = MutableStateFlow(firstAsset)
        val contexts = MutableStateFlow<BittensorEstimateContext?>(context)
        `when`(wallets.assetFlow(7L, chainAsset)).thenReturn(balances)
        val requests = mutableListOf<BittensorCostBasisRequest?>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            interactor.observeRequests(contexts).toList(requests)
        }
        balances.value = changedAsset
        contexts.value = null
        balances.value = firstAsset

        assertEquals(
            listOf(BittensorCostBasisRequest(context, BigInteger.ONE), BittensorCostBasisRequest(context, BigInteger.TEN), null),
            requests
        )
    }

    @Test
    fun `request uses its explicit wallet and preserves incomplete history`() = runTest {
        val coldKey = ByteArray(32) { 1 }
        val address = chain.addressOf(coldKey)
        val history = BittensorOperationHistory(emptyList(), allProviderPagesRead = false)
        `when`(accounts.getMetaAccount(7L)).thenReturn(account)
        `when`(account.accountIdIn(chain)).thenReturn(coldKey)
        `when`(historyRepository.history(address, BigInteger.ONE)).thenReturn(history)
        val request = BittensorCostBasisRequest(context, BigInteger.ONE)

        val basis = interactor.costBasis(request)

        assertFalse(basis.canEstimate(1))
        verify(accounts).getMetaAccount(7L)
        verify(historyRepository).history(address, BigInteger.ONE)
        verifyNoInteractions(wallets)
    }
}
