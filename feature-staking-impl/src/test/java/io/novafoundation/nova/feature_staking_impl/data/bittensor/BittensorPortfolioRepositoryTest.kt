package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger

class BittensorPortfolioRepositoryTest {

    private val registry = mock(ChainRegistry::class.java)
    private val accounts = mock(AccountRepository::class.java)
    private val wallet = mock(WalletRepository::class.java)
    private val chain = mock(Chain::class.java)
    private val chainAsset = mock(Chain.Asset::class.java)
    private val updates = BittensorPositionUpdates()
    private val swapSource = mock(BittensorSwapSource::class.java)
    private val logger = mock(Logger::class.java)
    private val asset = mock(Asset::class.java)
    private var positionRequests = 0
    private var failFirstWallet = false
    private val selected = MutableStateFlow(account(1L))
    private val positions = object : BittensorPositionSource {
        override suspend fun rootPositions(chainId: String, coldkey: ByteArray): List<BittensorRootPosition> {
            positionRequests++
            check(coldkey[0] == 1.toByte() && !failFirstWallet) { "RPC unavailable" }
            return listOf(BittensorRootPosition(AccountIdKey(ByteArray(32)), BigInteger.TEN))
        }

        override suspend fun subnetPositions(chainId: String, coldkey: ByteArray): List<BittensorSubnetPosition> = emptyList()
    }

    @Test
    fun `chart asset loads without querying unavailable positions`() = runBlocking {
        prepare()
        failFirstWallet = true

        assertEquals(asset, repository().observeAsset().first())
        assertEquals(0, positionRequests)
        verifyNoInteractions(swapSource, logger)
    }

    @Test
    fun `position error is logged and does not prevent reading chart asset`() = runBlocking {
        prepare()
        failFirstWallet = true
        val repository = repository()
        val portfolio = withTimeout(3_000L) {
            repository.observePortfolio().first { it.dataStatus is BittensorDataStatus.Stale }
        }

        assertNull(portfolio.positions)
        verify(logger).log("[BittensorPortfolio] Failed to refresh positions")
        verify(logger).log((portfolio.dataStatus as BittensorDataStatus.Stale).cause)
        assertEquals(asset, repository.observeAsset().first())
    }

    @Test
    fun `refresh failure preserves stake for the same wallet`() = runBlocking {
        prepare()
        val states = withTimeout(3_000L) {
            repository().observePortfolio().filter { it.dataStatus !is BittensorDataStatus.Loading }.onEach {
                if (it.dataStatus is BittensorDataStatus.Fresh) {
                    failFirstWallet = true
                    updates.refresh()
                }
            }.take(2).toList()
        }
        assertTrue(states[1].dataStatus is BittensorDataStatus.Stale)
        assertEquals(BigInteger.TEN, states[1].positions?.rootStake)
    }

    @Test
    fun `failed second wallet never receives the first wallet cached stake`() = runBlocking {
        prepare()
        val states = withTimeout(3_000L) {
            repository().observePortfolio().filter { it.dataStatus !is BittensorDataStatus.Loading }.onEach {
                if (it.metaId == 1L) selected.value = account(2L)
            }.take(2).toList()
        }
        assertEquals(2L, states[1].metaId)
        assertEquals(1.toByte(), states[0].coldkey.value[0])
        assertEquals(2.toByte(), states[1].coldkey.value[0])
        assertTrue(states[1].dataStatus is BittensorDataStatus.Stale)
        assertNull(states[1].positions)
    }

    private suspend fun prepare() {
        `when`(registry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(accounts.selectedMetaAccountFlow()).thenReturn(selected)
        `when`(wallet.assetFlow(1L, chainAsset)).thenReturn(flowOf(asset))
        `when`(wallet.assetFlow(2L, chainAsset)).thenReturn(flowOf(asset))
    }

    private fun account(id: Long): MetaAccount = mock(MetaAccount::class.java).also {
        `when`(it.id).thenReturn(id)
        `when`(it.accountIdIn(chain)).thenReturn(ByteArray(32) { id.toByte() })
    }

    private fun repository() = RealBittensorPortfolioRepository(registry, accounts, wallet, positions, swapSource, updates, logger)
}
