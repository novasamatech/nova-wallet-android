package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorStakingRepositoryTest {

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val accountRepository = mock(AccountRepository::class.java)

    private val account = mock(MetaAccount::class.java)

    private val walletRepository = mock(WalletRepository::class.java)

    private val asset = mock(Asset::class.java)

    private val chainStateRepository = mock(ChainStateRepository::class.java)

    private val logger = mock(Logger::class.java)

    private val blockNumbers = MutableStateFlow(BigInteger.ONE)

    private val validator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), "Validator")

    private var positionRequests = 0

    private var validatorRequests = 0

    private var failPositions = false

    private var failValidatorSelection = false

    private val positions = object : BittensorPositionSource {
        override suspend fun positions(chainId: String, coldKey: AccountId): BittensorStakePositions {
            positionRequests++
            check(!failPositions) { "Positions unavailable" }
            val root = BittensorRootPosition(validator.hotkey, BigInteger.valueOf(positionRequests.toLong()))
            return BittensorStakePositions(listOf(root), emptyList())
        }
    }

    private val validatorSource = object : BittensorRootValidatorSource {
        override suspend fun preferredValidator(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator {
            validatorRequests++
            check(!failValidatorSelection) { "Validators unavailable" }
            return validator
        }

        override suspend fun annualPercentageYield(): BigDecimal? = null
    }

    private val commonModule = CommonModule()

    private val computationalCache = commonModule.provideComputationalCache()

    private val swapSource = mock(BittensorSwapSource::class.java)

    private val portfolioLoader = BittensorPortfolioLoader(swapSource, logger)

    private val positionRepository = RealBittensorPositionRepository(positions, portfolioLoader, chainStateRepository, computationalCache, logger)

    private val repository = RealBittensorStakingRepository(
        chainRegistry,
        walletRepository,
        accountRepository,
        positionRepository,
        validatorSource,
        logger
    )

    @Test
    fun `balance growth updates root positions without reloading the validator directory on every block`() = runBlocking {
        prepare()

        withTimeout(3_000L) {
            repository.observeRootStakingDetails()
                .onEach { if (blockNumbers.value == BigInteger.ONE) blockNumbers.value += BigInteger.ONE }
                .take(2)
                .toList()
        }

        assertEquals(2, positionRequests)
        assertEquals(1, validatorRequests)
    }

    @Test
    fun `failed refresh keeps the default validator from cached positions`() = runBlocking {
        prepare()

        val states = withTimeout(3_000L) {
            repository.observeRootStakingDetails()
                .onEach {
                    failPositions = true
                    blockNumbers.value += BigInteger.ONE
                }
                .take(2)
                .toList()
        }

        assertTrue(states.first().dataStatus is BittensorDataStatus.Fresh)
        assertTrue(states.last().dataStatus is BittensorDataStatus.Stale)
        assertEquals(validator, states.last().validator)
        assertEquals(1, validatorRequests)
    }

    @Test
    fun `failed validator selection retries on the next block`() = runBlocking {
        prepare()
        failValidatorSelection = true

        val states = withTimeout(3_000L) {
            repository.observeRootStakingDetails()
                .onEach {
                    failValidatorSelection = false
                    blockNumbers.value += BigInteger.ONE
                }
                .take(2)
                .toList()
        }

        assertNull(states.first().validator)
        assertEquals(validator, states.last().validator)
        assertEquals(2, validatorRequests)
    }

    private suspend fun prepare() {
        `when`(chainRegistry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(accountRepository.selectedMetaAccountFlow()).thenReturn(flowOf(account))
        `when`(account.id).thenReturn(1L)
        `when`(account.accountIdIn(chain)).thenReturn(ByteArray(32))
        `when`(walletRepository.assetFlow(1L, chainAsset)).thenReturn(flowOf(asset))
        `when`(chainStateRepository.currentRemoteBlockNumberFlow(chain.id)).thenReturn(blockNumbers)
    }
}
