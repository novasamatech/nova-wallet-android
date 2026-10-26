package io.novafoundation.nova.feature_assets.data.network

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.BittensorBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetVisibilityRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AutoEnableTokensRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class ExternalBalanceVisibilityTest {

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val payments = mock(PaymentUpdaterFactory::class.java)

    private val locks = mock(BalanceLocksUpdaterFactory::class.java)

    private val pools = mock(PooledBalanceUpdaterFactory::class.java)

    private val bittensor = mock(BittensorBalanceUpdaterFactory::class.java)

    private val accountScope = mock(AccountUpdateScope::class.java)

    private val subscriptions = mock(StorageSharedRequestsBuilderFactory::class.java)

    private val walletRepository = mock(WalletRepository::class.java)

    private val autoEnable = mock(AutoEnableTokensRepository::class.java)

    private val externalBalances = mock(ExternalBalanceRepository::class.java)

    private val visibilityDelegate = mock(AssetVisibilityRepository::class.java)

    private val account = mock(MetaAccount::class.java)

    private val enabled = MutableStateFlow(true)

    private val chains = MutableStateFlow<List<Chain>>(emptyList())

    private val assetId = FullChainAssetId("chain", 0)

    private val shown = Channel<Set<FullChainAssetId>>(Channel.UNLIMITED)

    private val choices = mutableMapOf<FullChainAssetId, Boolean>()

    private val visibility = object : AssetVisibilityRepository by visibilityDelegate {
        override suspend fun showIfUndecided(metaId: Long, assetIds: Collection<FullChainAssetId>) {
            assertEquals(7L, metaId)
            assetIds.forEach { choices.putIfAbsent(it, true) }
            shown.send(assetIds.toSet())
        }
    }

    private val updateSystem = BalancesUpdateSystem(
        chainRegistry, payments, locks, pools, bittensor, accountScope, subscriptions,
        walletRepository, visibility, autoEnable, externalBalances
    )

    @Test
    fun `external stake reveals an undecided asset without liquid balance`() = runBlocking<Unit> {
        prepare()
        val job = launch { updateSystem.start().collect {} }
        try {
            withTimeout(3_000) { assertEquals(setOf(assetId), shown.receive()) }
            assertEquals(true, choices[assetId])
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `external stake preserves an explicitly hidden asset`() = runBlocking<Unit> {
        prepare()
        choices[assetId] = false
        val job = launch { updateSystem.start().collect {} }
        try {
            withTimeout(3_000) { shown.receive() }
            assertEquals(false, choices[assetId])
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `disabled discovery does not observe regular or external balances`() = runBlocking<Unit> {
        prepare()
        enabled.value = false
        val job = launch { updateSystem.start().collect {} }
        try {
            withTimeout(3_000) { enabled.subscriptionCount.first { it > 0 } }
            verifyNoInteractions(walletRepository, externalBalances)
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun prepare() {
        `when`(account.id).thenReturn(7L)
        `when`(accountScope.invalidationFlow()).thenReturn(flowOf(account))
        `when`(chainRegistry.currentChains).thenReturn(chains)
        `when`(autoEnable.autoEnableTokensFlow(7)).thenReturn(enabled)
        `when`(walletRepository.syncedAssetsFlow(7)).thenReturn(flowOf(emptyList()))
        val stake = ExternalBalance(assetId, BigInteger.TEN, ExternalBalance.Type.BITTENSOR)
        `when`(externalBalances.observeAccountExternalBalances(7)).thenReturn(flowOf(listOf(stake)))
    }
}
