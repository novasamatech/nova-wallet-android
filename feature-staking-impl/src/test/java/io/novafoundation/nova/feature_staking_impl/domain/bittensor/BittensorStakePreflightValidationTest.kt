package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.notValidOrNull
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorStakeValidationsModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorImmediateStakeValidation
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BITTENSOR_REQUEST_TIMEOUT
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

private const val REQUOTE_LATENCY_MILLIS = 50L

class BittensorStakePreflightValidationTest {

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val account = mock(MetaAccount::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val asset = mock(Asset::class.java)

    private val token = mock(Token::class.java)

    private val feeOrigin = SubmissionOrigin.singleOrigin(ByteArray(32))

    private val fee = SubstrateFee(BigInteger.TEN, feeOrigin, chainAsset)

    private val coldKey: AccountId = ByteArray(32) { 1 }

    private val hotkey: AccountId = ByteArray(32) { 2 }

    private var loadedColdKey: AccountId? = null

    private var snapshot = BittensorStakePreflight(true, true, true, false, false, BigInteger.ONE, BigInteger.TEN, BigInteger.ZERO, null)

    private var sourceFailure: Exception? = null

    private var loads = 0

    private var loadHangs = false

    private val repository = object : BittensorStakePreflightRepository {
        override suspend fun load(chainId: String, netuid: BittensorNetuid, coldKey: AccountId, hotkey: AccountId): BittensorStakePreflight {
            sourceFailure?.let { throw it }
            if (loadHangs) awaitCancellation()
            loads += 1
            loadedColdKey = coldKey
            return snapshot
        }
    }

    private val module = BittensorStakeValidationsModule()

    private val stakeValidationSystem = module.provideStakeValidationSystem()

    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)

    private val validators = mock(io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository::class.java)

    private val immediateExecution = mock(BittensorImmediateExecution::class.java)

    private val immediateValidation = BittensorImmediateStakeValidation()

    private val validation = BittensorStakePreflightValidation(
        repository, accounts, wallets, stakeValidationSystem, subnetInteractor, validators, immediateExecution, immediateValidation
    )

    private val system = ValidationSystem(validation)

    private val payload = BittensorStakePreflightPayload(chain, 7L, BittensorNetuid.Root, hotkey, BigInteger.valueOf(100_000_000), fee)

    @Before
    fun setup() = runBlocking<Unit> {
        `when`(chain.id).thenReturn("bittensor")
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(account.accountIdIn(chain)).thenReturn(coldKey)
        `when`(accounts.getMetaAccount(7L)).thenReturn(account)
        `when`(immediateExecution.support(account)).thenReturn(BittensorWalletSupport.SUPPORTED)
        `when`(wallets.assetFlow(7L, chainAsset)).thenReturn(flowOf(asset))
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.chainId).thenReturn("bittensor")
        `when`(asset.transferable).thenReturn(BigDecimal.ONE)
        `when`(asset.transferableInPlanks).thenReturn(BigInteger.TEN.pow(9))
    }

    @Test
    fun `direct root and subnet review reject delayed operation wallet before loading transaction data`() = runBlocking {
        `when`(immediateExecution.support(account)).thenReturn(BittensorWalletSupport.DELAYED_EXECUTION)
        for (netuid in listOf(BittensorNetuid.Root, BittensorNetuid.Subnet(1))) {
            val failure = system.validate(payload.copy(netuid = netuid)).getOrThrow().notValidOrNull()
            assertEquals(BittensorStakeValidationFailure.DELAYED_EXECUTION, failure?.reason)
        }
        assertEquals(0, loads)
        verify(accounts, never()).getSelectedMetaAccount()
        org.mockito.Mockito.verifyNoInteractions(wallets, subnetInteractor, validators)
    }

    @Test
    fun `proxied and delayed multisig wallets are refused before loading transaction data`() = runBlocking {
        val refusals = mapOf(
            BittensorWalletSupport.PROXIED_NOT_SUPPORTED to BittensorStakeValidationFailure.PROXIED_NOT_SUPPORTED,
            BittensorWalletSupport.DELAYED_EXECUTION to BittensorStakeValidationFailure.DELAYED_EXECUTION
        )
        for ((support, expected) in refusals) {
            `when`(immediateExecution.support(account)).thenReturn(support)

            assertEquals(expected, system.validate(payload).getOrThrow().notValidOrNull()?.reason)
        }
        assertEquals(0, loads)
        org.mockito.Mockito.verifyNoInteractions(wallets, subnetInteractor, validators)
    }

    @Test
    fun `warning continuation rechecks execution type of explicit wallet`() = runBlocking {
        assertNull(system.validate(payload).getOrThrow().notValidOrNull())
        `when`(immediateExecution.support(account)).thenReturn(BittensorWalletSupport.DELAYED_EXECUTION)
        val failure = system.validate(payload.copy(priceImpactAccepted = true)).getOrThrow().notValidOrNull()
        assertEquals(BittensorStakeValidationFailure.DELAYED_EXECUTION, failure?.reason)
        assertEquals(1, loads)
    }

    @Test
    fun `validation uses the operation wallet without reading selected wallet`() = runBlocking<Unit> {
        assertNull(system.validate(payload).getOrThrow().notValidOrNull())
        assertArrayEquals(coldKey, loadedColdKey)
        verify(accounts, never()).getSelectedMetaAccount()
        verify(wallets).assetFlow(7L, chainAsset)
    }

    @Test
    fun `revalidation reads current restrictions and balance even after warning acceptance`() = runBlocking<Unit> {
        assertNull(system.validate(payload).getOrThrow().notValidOrNull())
        snapshot = snapshot.copy(safeModeActive = true)
        val failure = system.validate(payload.copy(priceImpactAccepted = true)).getOrThrow().notValidOrNull()
        assertEquals(BittensorStakeValidationFailure.SAFE_MODE_ACTIVE, failure?.reason)
        assertEquals(2, loads)
        verify(wallets, times(2)).assetFlow(7L, chainAsset)
    }

    @Test
    fun `subnet validation reloads market after a warning without replacing the reviewed limit`() = runBlocking {
        val reviewedQuote = quote(capturedAtMillis = 0L)
        val latestQuote = quote(capturedAtMillis = System.currentTimeMillis())
        val subnetPayload = payload.copy(netuid = reviewedQuote.subnetId, quote = reviewedQuote)
        `when`(subnetInteractor.quote(reviewedQuote.subnetId, reviewedQuote.totalTaoInPlanks)).thenReturn(latestQuote)

        assertNull(system.validate(subnetPayload).getOrThrow().notValidOrNull())

        val movedQuote = latestQuote.copy(
            limitPrice = BigInteger.valueOf(1_500_000_000),
            simulation = latestQuote.simulation.copy(amountOut = BigInteger.valueOf(90_000_000))
        )
        `when`(subnetInteractor.quote(reviewedQuote.subnetId, reviewedQuote.totalTaoInPlanks)).thenReturn(movedQuote)
        val failure = system.validate(subnetPayload.copy(priceImpactAccepted = true))
            .getOrThrow()
            .notValidOrNull()

        assertEquals(BittensorStakeValidationFailure.SlippageExceeded(reviewedQuote, movedQuote), failure?.reason)
        assertEquals(BigInteger.valueOf(1_005_000_000), subnetPayload.quote?.limitPrice)
        assertEquals(2, loads)
    }

    @Test
    fun `market read failure cannot authorize an old reviewed quote`() = runBlocking {
        val quote = quote(System.currentTimeMillis())
        val subnetPayload = payload.copy(netuid = quote.subnetId, quote = quote)
        `when`(subnetInteractor.quote(quote.subnetId, quote.totalTaoInPlanks)).thenThrow(IllegalStateException("Market unavailable"))

        val failure = system.validate(subnetPayload).getOrThrow().notValidOrNull()

        assertEquals(BittensorStakeValidationFailure.QUOTE_UNAVAILABLE, failure?.reason)
    }

    @Test
    fun `market is reread at the chosen slippage`() = runBlocking<Unit> {
        val quote = quote(System.currentTimeMillis())
        val slippage = BittensorSlippage(BigDecimal("0.02"))
        `when`(subnetInteractor.quote(quote.subnetId, quote.totalTaoInPlanks, slippage)).thenReturn(quote)

        val status = system.validate(payload.copy(netuid = quote.subnetId, quote = quote, slippage = slippage)).getOrThrow()

        assertNull(status.notValidOrNull())
        verify(subnetInteractor).quote(quote.subnetId, quote.totalTaoInPlanks, slippage)
    }

    @Test
    fun `market reread that finishes after the other reads is still fresh`() = runBlocking {
        val reviewedQuote = quote(System.currentTimeMillis())
        val slippage = BittensorSlippage(BigDecimal("0.005"))
        val requoteStarted = CompletableDeferred<Unit>()
        // The re-read is the slowest read, like the network quote on a device: it suspends and is stamped when it arrives.
        `when`(subnetInteractor.quote(reviewedQuote.subnetId, reviewedQuote.totalTaoInPlanks, slippage)).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val continuation = invocation.rawArguments.last() as Continuation<BittensorSubnetBuyQuote>
            requoteStarted.complete(Unit)
            launch(Dispatchers.Default) {
                delay(REQUOTE_LATENCY_MILLIS)
                continuation.resume(quote(System.currentTimeMillis()))
            }
            COROUTINE_SUSPENDED
        }

        val status = system.validate(payload.copy(netuid = reviewedQuote.subnetId, quote = reviewedQuote, slippage = slippage)).getOrThrow()

        assertTrue(requoteStarted.isCompleted)
        assertNull(status.notValidOrNull())
    }

    @Test
    fun `existing inactive validator warns and rereads status after acceptance`() = runBlocking {
        val quote = quote(System.currentTimeMillis())
        val validator = BittensorValidator(io.novafoundation.nova.common.address.AccountIdKey(hotkey), chain.addressOf(hotkey))
        val row = BittensorSubnetValidator(validator, "address", BigDecimal.ZERO, false, inactive = true, canAddToExistingPosition = true)
        `when`(subnetInteractor.quote(quote.subnetId, quote.totalTaoInPlanks)).thenReturn(quote)
        `when`(validators.existingPositionValidator(chain, quote.subnetId, validator)).thenReturn(row)
        val existing = payload.copy(netuid = quote.subnetId, quote = quote, existingPosition = true)
        assertEquals(BittensorStakeValidationFailure.INACTIVE_VALIDATOR, system.validate(existing).getOrThrow().notValidOrNull()?.reason)
        assertNull(system.validate(existing.copy(inactiveValidatorAccepted = true)).getOrThrow().notValidOrNull())
        snapshot = snapshot.copy(safeModeActive = true)
        assertEquals(
            BittensorStakeValidationFailure.SAFE_MODE_ACTIVE,
            system.validate(existing.copy(inactiveValidatorAccepted = true)).getOrThrow().notValidOrNull()?.reason
        )
    }

    @Test
    fun `failed storage read never becomes permissive defaults`() = runBlocking {
        val failure = IOException("RPC unavailable")
        sourceFailure = failure
        val result = system.validate(payload)
        assertEquals(IOException::class.java, result.exceptionOrNull()?.javaClass)
        assertEquals(failure.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `chain read that never answers ends as a slow network failure`() = runTest {
        loadHangs = true

        // The validation itself, not the system: the system moves to Dispatchers.Default, off the virtual clock.
        val result = runCatching { validation.validate(payload) }

        assertEquals(BittensorTimeoutException::class.java, result.exceptionOrNull()?.javaClass)
        assertEquals(BITTENSOR_REQUEST_TIMEOUT, currentTime)
    }

    @Test
    fun `balance read that never answers ends as a slow network failure`() = runTest {
        `when`(wallets.assetFlow(7L, chainAsset)).thenReturn(flow { awaitCancellation() })

        // The validation itself, not the system: the system moves to Dispatchers.Default, off the virtual clock.
        val result = runCatching { validation.validate(payload) }

        assertEquals(BittensorTimeoutException::class.java, result.exceptionOrNull()?.javaClass)
        assertEquals(BITTENSOR_REQUEST_TIMEOUT, currentTime)
    }

    @Test
    fun `answering reads pass without waiting for the time bound`() = runTest {
        assertTrue(validation.validate(payload) is io.novafoundation.nova.common.validation.ValidationStatus.Valid)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `market reread that times out is a slow network failure, not missing liquidity`() = runBlocking {
        val quote = quote(System.currentTimeMillis())
        val slippage = BittensorSlippage(BigDecimal("0.005"))
        val timeout = runCatching { withTimeout(1) { awaitCancellation() } }.exceptionOrNull()
        `when`(subnetInteractor.quote(quote.subnetId, quote.totalTaoInPlanks, slippage)).thenThrow(timeout)

        val result = system.validate(payload.copy(netuid = quote.subnetId, quote = quote, slippage = slippage))

        assertEquals(BittensorTimeoutException::class.java, result.exceptionOrNull()?.javaClass)
    }

    private fun quote(capturedAtMillis: Long): BittensorSubnetBuyQuote {
        val subnetId = BittensorNetuid.Subnet(1)
        val amount = payload.amount
        val simulation = BittensorSwapSimulation(
            subnetId,
            BittensorSwapDirection.TAO_TO_ALPHA,
            amount,
            amount,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO
        )
        return BittensorSubnetBuyQuote(
            subnetId,
            amount,
            BigInteger.ZERO,
            simulation,
            BigInteger.valueOf(1_005_000_000),
            BigDecimal.ZERO,
            BigInteger.TEN.pow(9),
            capturedAtMillis
        )
    }
}
