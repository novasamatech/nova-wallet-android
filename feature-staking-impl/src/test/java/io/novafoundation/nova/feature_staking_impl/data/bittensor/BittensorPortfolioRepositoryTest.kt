package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class BittensorPortfolioRepositoryTest {

    private val registry = mock(ChainRegistry::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val signerProvider = mock(SignerProvider::class.java)

    private val signer = mock(NovaSigner::class.java)

    private val wallet = mock(WalletRepository::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val chainStateRepository = mock(ChainStateRepository::class.java)

    private val blockNumbers = MutableStateFlow(BigInteger.ZERO)

    private val priceRepository = mock(BittensorPortfolioPriceRepository::class.java)

    private val logger = mock(Logger::class.java)

    private val asset = mock(Asset::class.java)

    private var positionRequests = 0

    private var failFirstWallet = false

    private var rootStake = BigInteger.TEN

    private val selected = MutableStateFlow(account(1L))

    private val positions = object : BittensorPositionSource {
        override suspend fun positions(chainId: String, coldKey: AccountId): BittensorStakePositions {
            positionRequests++
            check(coldKey[0] == 1.toByte() && !failFirstWallet) { "RPC unavailable" }
            return BittensorStakePositions(listOf(BittensorRootPosition(AccountIdKey(ByteArray(32)), rootStake)), emptyList())
        }
    }

    @Test
    fun `chart asset loads without querying unavailable positions`() = runBlocking {
        prepare()
        failFirstWallet = true

        assertEquals(asset, repository().observeAsset().first())
        assertEquals(0, positionRequests)
        verifyNoInteractions(priceRepository, logger)
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
        verify(logger).log("[BittensorPositions] Failed to refresh positions")
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
                    blockNumbers.value += BigInteger.ONE
                }
            }.take(2).toList()
        }
        assertTrue(states[1].dataStatus is BittensorDataStatus.Stale)
        assertEquals(BigInteger.TEN, states[1].positions?.rootStake)
    }

    @Test
    fun `new block updates positions without a transaction refresh signal`() = runBlocking {
        prepare()
        val states = withTimeout(3_000L) {
            repository().observePortfolio().filter { it.dataStatus is BittensorDataStatus.Fresh }.onEach {
                rootStake = BigInteger.valueOf(20)
                blockNumbers.value += BigInteger.ONE
            }.take(2).toList()
        }

        assertEquals(listOf(BigInteger.TEN, BigInteger.valueOf(20)), states.map { it.positions?.rootStake })
    }

    @Test
    fun `next block recovers after a failed position request`() = runBlocking {
        prepare()
        failFirstWallet = true
        val states = withTimeout(3_000L) {
            repository().observePortfolio().filter { it.dataStatus !is BittensorDataStatus.Loading }.onEach {
                if (it.dataStatus is BittensorDataStatus.Stale) {
                    failFirstWallet = false
                    blockNumbers.value += BigInteger.ONE
                }
            }.take(2).toList()
        }

        assertTrue(states[0].dataStatus is BittensorDataStatus.Stale)
        assertTrue(states[1].dataStatus is BittensorDataStatus.Fresh)
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
        assertTrue(states[1].dataStatus is BittensorDataStatus.Stale)
        assertNull(states[1].positions)
    }

    @Test
    fun `immediate multisig enables portfolio and root actions`() = runBlocking {
        prepare()
        `when`(selected.value.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        val repository = repository()

        assertTrue(repository.observePortfolio().first().supportsImmediateOperations)
        assertTrue(repository.observeRootPortfolio().first().supportsImmediateOperations)
    }

    @Test
    fun `delayed multisig disables portfolio and root actions`() = runBlocking {
        prepare()
        `when`(selected.value.type).thenReturn(LightMetaAccount.Type.MULTISIG)
        `when`(signer.callExecutionType()).thenReturn(CallExecutionType.DELAYED)
        val repository = repository()

        assertFalse(repository.observePortfolio().first().supportsImmediateOperations)
        assertFalse(repository.observeRootPortfolio().first().supportsImmediateOperations)
    }

    private suspend fun prepare() {
        `when`(signer.callExecutionType()).thenReturn(CallExecutionType.IMMEDIATE)
        `when`(registry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(chainStateRepository.currentRemoteBlockNumberFlow(chain.id)).thenReturn(blockNumbers)
        `when`(accounts.selectedMetaAccountFlow()).thenReturn(selected)
        `when`(wallet.assetFlow(1L, chainAsset)).thenReturn(flowOf(asset))
        `when`(wallet.assetFlow(2L, chainAsset)).thenReturn(flowOf(asset))
    }

    private fun account(id: Long): MetaAccount = mock(MetaAccount::class.java).also {
        `when`(signerProvider.rootSignerFor(it)).thenReturn(signer)
        `when`(it.id).thenReturn(id)
        `when`(it.accountIdIn(chain)).thenReturn(ByteArray(32) { id.toByte() })
    }

    private val portfolioLoader = BittensorPortfolioLoader(priceRepository, logger)

    private val commonModule = CommonModule()

    private val computationalCache = commonModule.provideComputationalCache()

    private val positionRepository = RealBittensorPositionRepository(positions, portfolioLoader, chainStateRepository, computationalCache, logger)

    private fun repository() = RealBittensorPortfolioRepository(registry, accounts, wallet, positionRepository, signerProvider)
}
