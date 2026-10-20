package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorExitValidationsModule
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicBuildingContext
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfig
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitOperationTestFixture
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapQuote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.toPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.RETURNS_DEFAULTS

class BittensorExitQuoteInteractorTest {

    private val subnet = BittensorNetuid.Subnet(64)

    private val amount = BigInteger.valueOf(1_000_000)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private var partial = false

    private var cancelled = false

    private var quoteCalls = 0

    private val source = object : BittensorSwapSource {
        override suspend fun quote(
            chainId: String,
            subnetId: BittensorNetuid.Subnet,
            direction: BittensorSwapDirection,
            amountIn: BigInteger
        ): BittensorSwapQuote {
            if (cancelled) throw CancellationException("Replaced")
            quoteCalls++
            assertEquals("bittensor", chainId)
            assertEquals(subnet, subnetId)
            assertEquals(BittensorSwapDirection.ALPHA_TO_TAO, direction)
            assertEquals(amount, amountIn)
            val simulation = BittensorSwapSimulation(
                subnetId,
                direction,
                amountIn - BigInteger.TEN - if (partial) BigInteger.ONE else BigInteger.ZERO,
                amountIn - BigInteger.TEN,
                BigInteger.ZERO,
                BigInteger.TEN,
                BigInteger.ZERO,
                BigInteger.ZERO
            )
            return BittensorSwapQuote("0xpinned", simulation, BittensorProtocolUnits.RAO_PER_TAO)
        }

        override suspend fun simulate(
            chainId: String,
            subnetId: BittensorNetuid.Subnet,
            direction: BittensorSwapDirection,
            amountIn: BigInteger
        ): BittensorSwapSimulation = error("Unpinned simulation requested")

        override suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorNetuid.Subnet): BigInteger = error("Unpinned price requested")
    }

    private val commission = BittensorSubnetCommission(BigDecimal("0.003"))

    private val config = BittensorConfigRepository { BittensorConfig(commission, emptyMap()) }

    private val registry = mock(ChainRegistry::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val runtime = mock(BittensorExitRuntimeSource::class.java)

    private val metadata = mock(BittensorSubnetMetadataInteractor::class.java)

    private val slippage = BittensorSlippage(BigDecimal("0.005"))

    private val loadedFee = mock(Fee::class.java)

    private val estimates = mutableListOf<FormExtrinsicWithOrigin>()

    private val extrinsics = mock(ExtrinsicService::class.java) { invocation ->
        if (invocation.method.name == "estimateFee") {
            estimates += invocation.getArgument<FormExtrinsicWithOrigin>(3)
            loadedFee
        } else {
            RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private val executor = mock(BittensorSubmissionExecutor::class.java)

    private val constants = mock(WalletConstants::class.java)

    private val logger = mock(Logger::class.java)

    private val immediate = mock(BittensorImmediateExecution::class.java)

    private val chain = mock(Chain::class.java)

    private val asset = mock(Asset::class.java)

    private val preflight = BittensorExitPreflight(
        amount * BigInteger.TEN,
        BittensorExitAvailability(amount * BigInteger.TEN, BigInteger.ZERO, amount * BigInteger.TEN),
        BigInteger.ZERO,
        false,
        BigInteger.ONE,
        BigInteger.ONE,
        false
    )

    private val position = BittensorExitPosition(1, chain, hotkey, hotkey, "validator", subnet, asset, null, preflight, true, BigInteger.ONE)

    private val costBasis = mock(BittensorCostBasisInteractor::class.java)

    private val chainState = mock(io.novafoundation.nova.runtime.repository.ChainStateRepository::class.java)

    private val interactor = BittensorExitInteractor(
        registry, accounts, wallets, runtime, metadata, source, config, slippage, extrinsics, executor, constants,
        "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY", logger, immediate, costBasis, chainState
    )

    @Test
    fun `exit observation refreshes holds collateral and balances without reopening`() = runBlocking {
        val wallet = mock(MetaAccount::class.java)
        val token = mock(Token::class.java)
        val chainAsset = mock(Chain.Asset::class.java)
        val updatedAsset = mock(Asset::class.java)
        val blocks = MutableStateFlow(BigInteger.ONE)
        val assets = MutableStateFlow(asset)
        val address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"
        `when`(chain.id).thenReturn("bittensor")
        `when`(chain.assets).thenReturn(listOf(chainAsset))
        `when`(chainAsset.id).thenReturn(0)
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(registry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(accounts.getMetaAccount(1)).thenReturn(wallet)
        `when`(wallet.id).thenReturn(1L)
        `when`(wallet.type).thenReturn(LightMetaAccount.Type.SECRETS)
        `when`(wallet.accountIdIn(chain)).thenReturn(hotkey.value)
        `when`(wallets.assetFlow(1, chainAsset)).thenReturn(assets)
        `when`(constants.existentialDeposit(chain.id)).thenReturn(BigInteger.ONE)
        `when`(chainState.currentRemoteBlockNumberFlow(chain.id)).thenReturn(blocks)
        val validator = AccountIdKey(chain.accountIdOf(address))
        val member = BittensorExitMember(amount, BigInteger.ZERO, false, BigInteger.ONE)
        var snapshot = preflight.copy(
            rootHoldRemaining = BigInteger.ONE,
            groupStakes = mapOf(validator to amount),
            groupMembers = mapOf(validator to member)
        )
        `when`(runtime.preflight(chain.id, hotkey.value, validator.value, BittensorNetuid.Root)).thenAnswer { snapshot }
        val received = Channel<BittensorExitPosition>(Channel.UNLIMITED)
        val observation = launch {
            interactor.observe(1, BittensorNetuid.Root, address).collect { received.send(it) }
        }
        try {
            val held = withTimeout(2_000) { received.receive() }
            assertEquals(BigInteger.ZERO, interactor.maximumAmount(held))
            snapshot = snapshot.copy(
                rootHoldRemaining = BigInteger.ZERO,
                groupMembers = mapOf(validator to member.copy(holdRemaining = BigInteger.ZERO))
            )
            blocks.value = BigInteger.TWO
            val unlocked = withTimeout(2_000) { received.receive() }
            assertEquals(amount, interactor.maximumAmount(unlocked))
            snapshot = snapshot.copy(groupMembers = mapOf(validator to member.copy(holdRemaining = BigInteger.ZERO, collateral = amount / BigInteger.TWO)))
            blocks.value = BigInteger.valueOf(3)
            val collateral = withTimeout(2_000) { received.receive() }
            assertEquals(amount / BigInteger.TWO, interactor.maximumAmount(collateral))
            assets.value = updatedAsset
            val funded = withTimeout(2_000) { received.receive() }
            assertSame(updatedAsset, funded.asset)
            assertEquals(collateral.preflight, funded.preflight)
        } finally {
            observation.cancelAndJoin()
        }
    }

    @Test
    fun `sell and explicit refresh use only pinned simulation and spot price`() = runBlocking {
        `when`(chain.id).thenReturn("bittensor")
        val reviewed = interactor.quote(position, amount, false)
        assertEquals(BigInteger.valueOf(995_000_000), reviewed.limitPrice)
        assertEquals(amount - BigInteger.TEN, reviewed.estimatedTaoOut)
        assertEquals(commission.feeFor(reviewed.estimatedTaoOut), reviewed.serviceFee)
        assertEquals(commission.fraction, reviewed.serviceFeeRate)
        assertEquals(reviewed.serviceFeeRate, reviewed.toPayload(BittensorExitPositionPayload(1, subnet.value, "validator")).quote().serviceFeeRate)
        val refreshed = interactor.refreshQuote(position, reviewed)
        assertEquals(reviewed, refreshed)
        assertEquals(2, quoteCalls)
    }

    @Test
    fun `partial fill fails closed`() = runBlocking {
        `when`(chain.id).thenReturn("bittensor")
        partial = true
        assertTrue(runCatching { interactor.quote(position, amount, false) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `sell cancellation propagates`() = runBlocking {
        `when`(chain.id).thenReturn("bittensor")
        cancelled = true
        assertTrue(runCatching { interactor.quote(position, amount, false) }.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `captured slippage survives refresh and payload round trip`() = runBlocking {
        `when`(chain.id).thenReturn("bittensor")
        val fraction = BigDecimal("0.03")
        val reviewed = interactor.quote(position, amount, false, fraction)
        assertEquals(BigInteger.valueOf(970_000_000), reviewed.limitPrice)
        assertEquals(fraction, interactor.refreshQuote(position, reviewed).slippageFraction)
        assertEquals(reviewed, reviewed.toPayload(BittensorExitPositionPayload(1, subnet.value, "validator")).quote())
    }

    @Test
    fun `selected slippage recalculates drawable collateral floor for max`() {
        val stake = amount * BigInteger.TEN
        val free = BigInteger.valueOf(102_000)
        val member = BittensorExitMember(stake, stake - free, false, BigInteger.ZERO)
        val basis = preflight.copy(
            minStake = BigInteger.valueOf(100_000),
            groupStakes = mapOf(hotkey to stake),
            groupMembers = mapOf(hotkey to member),
            withdrawalPrice = BittensorProtocolUnits.RAO_PER_TAO
        )
        val collateralPosition = position.copy(preflight = basis)
        assertEquals(free, interactor.maximumAmount(collateralPosition, BigDecimal("0.005")))
        assertEquals(BigInteger.ZERO, interactor.maximumAmount(collateralPosition, BigDecimal("0.03")))
    }

    @Test
    fun `net receive deducts commission and network fee once after runtime fee`() = runBlocking {
        `when`(chain.id).thenReturn("bittensor")
        val reviewed = interactor.quote(position, amount, false)
        val token = mock(Token::class.java)
        val chainAsset = mock(Chain.Asset::class.java)
        val fee = mock(Fee::class.java)
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(fee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger.valueOf(500))
        val operation = BittensorExitOperation(reviewed, hotkey, hotkey)
        val draft = BittensorExitDraft(position, reviewed, { operation }, fee, false)
        assertEquals(amount - BigInteger.TEN - reviewed.serviceFee - BigInteger.valueOf(500), draft.netReceive)
    }

    @Test
    fun `root partial estimates gross once then prepares requested minus loaded fee`() = runBlocking {
        configureLoadedFee()
        val root = position.copy(netuid = BittensorNetuid.Root)
        val quote = interactor.quote(root, amount, false)
        val estimated = estimateCalls(root, quote)
        assertEquals(amount, estimated.single().arguments["amount_unstaked"])

        val draft = interactor.prepare(root, quote, loadedFee, feeFromStake = true)
        val prepared = operationCalls(draft.operation)
        assertEquals(amount - BigInteger.valueOf(500), prepared.single().arguments["amount_unstaked"])
        assertEquals(amount, draft.quote.amount)
        assertSame(loadedFee, draft.fee)
        assertEquals(1, estimates.size) // Preparation must not estimate the adjusted amount again.
    }

    @Test
    fun `root requests at or below loaded fee remain drafts and fail standard fee validation`() = runBlocking {
        configureLoadedFee()
        val root = position.copy(netuid = BittensorNetuid.Root)
        val module = BittensorExitValidationsModule()
        val validations = module.provideExitValidationSystem()
        for (requested in listOf(BigInteger.valueOf(499), BigInteger.valueOf(500))) {
            val quote = interactor.quote(root, requested, false)
            val draft = interactor.prepare(root, quote, loadedFee, feeFromStake = true)
            assertEquals(requested, draft.quote.amount)
            assertEquals(BigInteger.ZERO, draft.netReceive)
            val payload = BittensorExitValidationPayload(
                preflight = root.preflight,
                quote = draft.quote,
                transferable = BigInteger.ZERO,
                fee = BigInteger.valueOf(500),
                existentialDeposit = BigInteger.ONE,
                supportsFeeFromStake = true,
                expectedFeeFromStake = true
            )
            val failure = validations.validate(payload).getOrThrow() as ValidationStatus.NotValid
            assertEquals(BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE, failure.reason)
            // A caller bypassing validation still cannot construct/sign an invalid operation.
            assertTrue(runCatching { draft.operation }.exceptionOrNull() is IllegalArgumentException)
        }
        assertTrue(mockingDetails(executor).invocations.isEmpty())
    }

    @Test
    fun `quote refresh preserves complete group identity and captured slippage`() = runBlocking {
        `when`(chain.id).thenReturn("bittensor")
        val second = AccountIdKey(ByteArray(32) { 2 })
        val selectedSlippage = BigDecimal("0.01")
        val reviewed = interactor.quote(position, amount, false, selectedSlippage)
            .copy(groupHotkeys = setOf(hotkey, second))
        val refreshed = interactor.refreshQuote(position, reviewed)
        val restored = refreshed.toPayload(BittensorExitPositionPayload(1, subnet.value, "hotkey")).quote()
        assertEquals(reviewed.groupHotkeys, restored.groupHotkeys)
        assertEquals(reviewed.plan, restored.plan)
        assertEquals(selectedSlippage, restored.slippageFraction)
    }

    @Test
    fun `root partial paid from free keeps gross estimated call amount`() = runBlocking {
        configureLoadedFee()
        val root = position.copy(netuid = BittensorNetuid.Root)
        val quote = interactor.quote(root, amount, false)
        val estimated = estimateCalls(root, quote)
        val draft = interactor.prepare(root, quote, loadedFee, feeFromStake = false)
        assertEquals(estimated.single().arguments, operationCalls(draft.operation).single().arguments)
        assertEquals(1, estimates.size)
    }

    @Test
    fun `root full exit remains full in single estimate and prepared call`() = runBlocking {
        configureLoadedFee()
        val root = position.copy(netuid = BittensorNetuid.Root)
        val quote = interactor.quote(root, amount, true)
        val estimated = estimateCalls(root, quote)
        val draft = interactor.prepare(root, quote, loadedFee, feeFromStake = true)
        assertEquals("remove_stake_full_limit", estimated.single().function.name)
        assertEquals(estimated.single().arguments, operationCalls(draft.operation).single().arguments)
        assertEquals(1, estimates.size)
    }

    @Test
    fun `root group estimate and prepared slices remain unchanged`() = runBlocking {
        configureLoadedFee()
        val second = AccountIdKey(ByteArray(32) { 2 })
        val members = mapOf(
            hotkey to BittensorExitMember(amount, BigInteger.ZERO, false, BigInteger.ZERO),
            second to BittensorExitMember(amount, BigInteger.ZERO, false, BigInteger.ZERO)
        )
        val basis = preflight.copy(groupStakes = members.mapValues { it.value.stake }, groupMembers = members)
        val root = position.copy(netuid = BittensorNetuid.Root, preflight = basis)
        val quote = interactor.quote(root, amount + amount / BigInteger.TWO, false)
        val estimated = estimateCalls(root, quote)
        val draft = interactor.prepare(root, quote, loadedFee, feeFromStake = false)
        val prepared = operationCalls(draft.operation)
        assertEquals(listOf("remove_stake_full_limit", "remove_stake"), estimated.map { it.function.name })
        assertEquals(estimated.map { it.arguments }, prepared.map { it.arguments })
        assertEquals(amount / BigInteger.TWO, prepared.last().arguments["amount_unstaked"])
        assertEquals(1, estimates.size)
    }

    private fun configureLoadedFee() {
        val token = mock(Token::class.java)
        val chainAsset = mock(Chain.Asset::class.java)
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(loadedFee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger.valueOf(500))
    }

    private suspend fun estimateCalls(position: BittensorExitPosition, quote: BittensorExitQuote): List<GenericCall.Instance> {
        assertSame(loadedFee, interactor.estimateFee(position, quote))
        assertEquals(1, estimates.size)
        val builder = BittensorExitOperationTestFixture.builder()
        val context = mock(ExtrinsicBuildingContext::class.java)
        estimates.single().invoke(builder, context)
        return calls(builder)
    }

    private fun operationCalls(operation: BittensorExitOperation): List<GenericCall.Instance> {
        val builder = BittensorExitOperationTestFixture.builder()
        operation.build(builder)
        return calls(builder)
    }

    private fun calls(builder: ExtrinsicBuilder): List<GenericCall.Instance> = mockingDetails(builder).invocations
        .filter { it.method.name == "call" }
        .map { it.getArgument(0) }
}
