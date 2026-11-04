package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitBasisPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitExceedsStakeException
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitFloors
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitGroup
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitSelection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSlippage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorRootHoldFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeContext
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class BittensorExitAmountViewModelTest(private val netuid: Int, private val nativePrecision: Int) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "netuid={0}, nativePrecision={1}")
        fun flows() = listOf(arrayOf(0, 9), arrayOf(1, 12))
    }

    private val analytics = Mockito.mock(AnalyticsService::class.java)

    private val resources = Mockito.mock(ResourceManager::class.java)

    private val logger = Mockito.mock(Logger::class.java)

    private val interactionGate = Mockito.mock(AutomaticInteractionGate::class.java)

    private val retries = BittensorRetryController(resources, logger, interactionGate)

    private val interactor = Mockito.mock(BittensorExitInteractor::class.java)

    private val router = Mockito.mock(StakingRouter::class.java)

    private val assetIcons = Mockito.mock(AssetIconProvider::class.java)

    private val addressIcons = Mockito.mock(AddressIconGenerator::class.java)

    private val tokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence = amount.toPlainString()
    }

    private val fiatFormatter = Mockito.mock(FiatFormatter::class.java)

    private val externalActions = Mockito.mock(ExternalActions.Presentation::class.java)

    private val estimates = Mockito.mock(BittensorEstimatesMixin::class.java)

    private val validationExecutor = ValidationExecutor()

    @Suppress("UNCHECKED_CAST")
    private val validationSystem = Mockito.mock(ValidationSystem::class.java) as BittensorExitValidationSystem

    private val exitFormatter = Mockito.mock(BittensorExitFormatter::class.java)

    private val slippageSettings = Mockito.mock(BittensorSlippageSettings::class.java)

    private val slippage = BittensorSlippage(BigDecimal("0.005"))

    private val costBasisFormatter = BittensorSaleCostBasisFormatter(resources, tokenFormatter, fiatFormatter)

    private val precisionValidator = BittensorExitPrecisionValidator(resources)

    private val amountValidator = BittensorExitAmountValidator(resources, precisionValidator)

    private val validators = Mockito.mock(BittensorValidatorInteractor::class.java)

    private val rootHoldFormatter = Mockito.mock(BittensorRootHoldFormatter::class.java)

    private val actionFactory = Mockito.mock(ActionAwaitableMixin.Factory::class.java)

    private val chain = Mockito.mock(Chain::class.java)

    private val chainAsset = Mockito.mock(Chain.Asset::class.java)

    private val token = Mockito.mock(Token::class.java)

    private val asset = Mockito.mock(Asset::class.java)

    private val preflight = Mockito.mock(BittensorExitPreflight::class.java)

    private val positions = MutableSharedFlow<BittensorExitPosition>(replay = 1)

    private val groups = ConcurrentHashMap<BittensorExitPosition, BittensorExitGroup>()

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val secondHotkey = AccountIdKey(ByteArray(32) { 2 })

    private val payload = BittensorExitPositionPayload(1L, netuid, "validator")

    private val feeState = MutableStateFlow<FeeStatus<BittensorFeeEstimate<BittensorExitRequest>, FeeDisplay>>(
        FeeStatus.Loading(visibleDuringProgress = true)
    )

    private val feeLoader = Mockito.mock(FeeLoaderMixinV2.Presentation::class.java)

    private val unusedFeeFactory = Mockito.mock(FeeLoaderMixinV2.Factory::class.java)

    private val feeFactory = object : FeeLoaderMixinV2.Factory by unusedFeeFactory {
        @Suppress("UNCHECKED_CAST")
        override fun <F : SubmissionFee> createDefault(
            scope: CoroutineScope,
            feeContextFlow: Flow<FeeContext>,
            configuration: FeeLoaderMixinV2.Configuration<F, FeeDisplay>
        ): FeeLoaderMixinV2.Presentation<F, FeeDisplay> = feeLoader as FeeLoaderMixinV2.Presentation<F, FeeDisplay>
    }

    private lateinit var viewModel: BittensorExitAmountViewModel

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        stubResources()
        stubAsset()
        stubInteractor()
        Mockito.`when`(feeLoader.fee).thenReturn(feeState)
        Mockito.`when`(slippageSettings.observe()).thenReturn(flowOf(slippage.fraction))
        Mockito.`when`(estimates.observePositionCostBasis(Mockito.any<Flow<BittensorEstimateContext?>>() ?: emptyFlow()))
            .thenReturn(emptyFlow())
        viewModel = createViewModel()
    }

    @After
    fun tearDown() = runBlocking<Unit> {
        if (::viewModel.isInitialized) viewModel.coroutineContext.job.cancelAndJoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `overflow shows the same field and button error and Continue cannot navigate`() = runBlocking<Unit> {
        withTimeout(5_000) {
            publish(group(1_000_000_000))
            viewModel.amountChanged("1.000000001")
            awaitError("Not enough tokens")
            viewModel.continueClicked()
            Mockito.verifyNoInteractions(router, validationSystem)
            assertEquals("1.000000001", viewModel.amount.first())

            viewModel.amountChanged("1")
            viewModel.amountError.first { it === FieldValidationResult.Ok }
            viewModel.amountChanged("")
            awaitButton(DescriptiveButtonState.Disabled("Enter amount"))
        }
    }

    @Test
    fun `precision error disables Continue using position precision even when native precision differs`() = runBlocking<Unit> {
        withTimeout(5_000) {
            publish(group(1_000_000_000))
            viewModel.amountChanged("0.0000000001")
            awaitError("Use 9 decimal places")
            viewModel.continueClicked()
            Mockito.verifyNoInteractions(router, validationSystem)
        }
    }

    @Test
    fun `positive overflow wins over fully locked while blank and zero keep nothing available`() = runBlocking<Unit> {
        withTimeout(5_000) {
            publish(group(1_000_000_000, available = 0))
            awaitButton(DescriptiveButtonState.Disabled("Nothing available"))
            viewModel.amountChanged("1")
            awaitError("Not enough tokens")

            for (text in listOf("", "0")) {
                viewModel.amountChanged(text)
                viewModel.amountError.first { it === FieldValidationResult.Ok }
                awaitButton(DescriptiveButtonState.Disabled("Nothing available"))
            }
        }
    }

    @Test
    fun `unknown maximum leaves field valid and Continue loading`() = runBlocking<Unit> {
        withTimeout(5_000) {
            viewModel.amountChanged("100")
            viewModel.amount.first { it == "100" }
            assertSame(FieldValidationResult.Ok, viewModel.amountError.first())
            awaitButton(DescriptiveButtonState.Loading)
            viewModel.continueClicked()
            Mockito.verifyNoInteractions(router, validationSystem)
        }
    }

    @Test
    fun `live maximum changes revalidate unchanged amount`() = runBlocking<Unit> {
        withTimeout(5_000) {
            publish(group(1_000_000_000))
            viewModel.amountChanged("0.75")
            viewModel.amount.first { it == "0.75" }
            publish(group(1_000_000_000, available = 749_999_999))
            awaitError("Not enough tokens")

            publish(group(1_000_000_000, available = 750_000_000))
            viewModel.amountError.first { it === FieldValidationResult.Ok }
            assertEquals("0.75", viewModel.amount.first())
        }
    }

    @Test
    fun `display maximum validates above smaller dust plan fill and Max keeps its live intent`() = runBlocking<Unit> {
        withTimeout(5_000) {
            val primary = basisPosition(hotkey, 1_000)
            val secondary = basisPosition(secondHotkey, 100)
            val floors = BittensorExitFloors(BigInteger.ONE, BigInteger.valueOf(100))
            val dustGroup = BittensorExitGroup(listOf(primary, secondary), BigInteger.valueOf(1_050), floors)
            assertEquals(BigInteger.valueOf(1_050), dustGroup.maximum(BittensorExitSelection.All))
            assertEquals(BigInteger.valueOf(1_000), dustGroup.maximumFill(BittensorExitSelection.All))
            publish(dustGroup)

            viewModel.amountChanged("0.000001051")
            awaitError("Not enough tokens")
            viewModel.amountChanged("0.00000105")
            viewModel.amountError.first { it === FieldValidationResult.Ok }
            viewModel.maxClicked()
            viewModel.amount.first { it == "0.000001" }

            publish(group(2_000))
            viewModel.amount.first { it == "0.000002" }
            viewModel.amountChanged("")
            viewModel.amount.first { it.isEmpty() }
            publish(group(3_000))
            awaitButton(DescriptiveButtonState.Disabled("Enter amount"))
            assertEquals("", viewModel.amount.first())
        }
    }

    private fun stubResources() {
        Mockito.`when`(resources.getString(R.string.common_continue)).thenReturn("Continue")
        Mockito.`when`(resources.getString(R.string.common_enter_amount)).thenReturn("Enter amount")
        Mockito.`when`(resources.getString(R.string.common_error_not_enough_tokens)).thenReturn("Not enough tokens")
        Mockito.`when`(resources.getString(R.string.bittensor_exit_nothing_available_title)).thenReturn("Nothing available")
        Mockito.`when`(resources.getString(R.string.bittensor_amount_precision_error, 9)).thenReturn("Use 9 decimal places")
    }

    private fun stubAsset() {
        Mockito.`when`(asset.token).thenReturn(token)
        Mockito.`when`(token.configuration).thenReturn(chainAsset)
        Mockito.`when`(chainAsset.precision).thenReturn(Precision(nativePrecision))
        Mockito.`when`(chainAsset.symbol).thenReturn(TokenSymbol("TAO"))
    }

    private suspend fun stubInteractor() {
        Mockito.`when`(interactor.slippage).thenReturn(slippage)
        Mockito.`when`(interactor.observe(
            Mockito.eq(1L),
            Mockito.eq(payload.netuid()) ?: payload.netuid(),
            Mockito.eq("validator") ?: "validator",
            Mockito.any<Flow<Boolean>>() ?: emptyFlow()
        ))
            .thenReturn(positions)
        Mockito.`when`(interactor.requotes(Mockito.any<Flow<Boolean>>() ?: emptyFlow(), Mockito.any<Flow<Unit>>() ?: emptyFlow()))
            .thenReturn(emptyFlow())
        Mockito.`when`(interactor.observeSubnetLogos()).thenReturn(flowOf(emptyMap()))
        Mockito.`when`(interactor.exitGroup(
            Mockito.any(BittensorExitPosition::class.java) ?: position(),
            Mockito.eq(slippage.fraction) ?: slippage.fraction
        ))
            .thenAnswer { groups.getValue(it.getArgument(0)) }
        Mockito.`when`(validators.validatorNames(
            Mockito.eq(payload.netuid()) ?: payload.netuid(),
            Mockito.anySet<AccountIdKey>() ?: emptySet()
        ))
            .thenReturn(mapOf(hotkey to "Validator", secondHotkey to "Second validator"))
        Mockito.`when`(interactor.quote(
            Mockito.any(BittensorExitPosition::class.java) ?: position(),
            Mockito.any(BigInteger::class.java) ?: BigInteger.ONE,
            Mockito.any(BittensorExitSelection::class.java) ?: BittensorExitSelection.All,
            Mockito.eq(slippage.fraction) ?: slippage.fraction
        )).thenThrow(BittensorExitExceedsStakeException())
    }

    private fun createViewModel() = BittensorExitAmountViewModel(
        analytics = analytics,
        payload = payload,
        bittensorExitInteractor = interactor,
        router = router,
        resources = resources,
        assetIconProvider = assetIcons,
        addressIconGenerator = addressIcons,
        tokenFormatter = tokenFormatter,
        fiatFormatter = fiatFormatter,
        externalActions = externalActions,
        estimatesMixin = estimates,
        retryController = retries,
        validationExecutor = validationExecutor,
        exitValidationSystem = validationSystem,
        feeLoaderMixinFactory = feeFactory,
        exitFormatter = exitFormatter,
        slippageSettings = slippageSettings,
        costBasisFormatter = costBasisFormatter,
        exitAmountValidator = amountValidator,
        validatorInteractor = validators,
        rootHoldFormatter = rootHoldFormatter,
        actionAwaitableMixinFactory = actionFactory
    )

    private fun position() = BittensorExitPosition(
        metaId = 1L,
        chain = chain,
        coldkey = secondHotkey,
        hotkey = hotkey,
        validatorAddress = "validator",
        netuid = payload.netuid(),
        asset = asset,
        subnet = if (netuid == 0) null else BittensorSubnet(BittensorNetuid.Subnet(netuid), "Subnet", "ALPHA", null),
        preflight = preflight,
        supportsFeeFromStake = true,
        existentialDeposit = BigInteger.ZERO
    )

    private fun basisPosition(hotkey: AccountIdKey, alpha: Long) = BittensorExitBasisPosition(
        hotkey, BigInteger.valueOf(alpha), BigInteger.ZERO, isOwned = false, isHeld = false
    )

    private fun group(alpha: Long, available: Long = alpha): BittensorExitGroup {
        val member = basisPosition(hotkey, alpha)
        val floors = BittensorExitFloors(BigInteger.ONE, BigInteger.ONE)
        return BittensorExitGroup(listOf(member), BigInteger.valueOf(available), floors)
    }

    private suspend fun publish(group: BittensorExitGroup) {
        // A different snapshot identity makes this a real position update for StateFlow/combine.
        val position = position().copy(existentialDeposit = BigInteger.valueOf(groups.size.toLong()))
        groups[position] = group
        positions.emit(position)
        viewModel.position.first { it === position }
    }

    private suspend fun awaitError(reason: String) {
        val error = viewModel.amountError.first { it is FieldValidationResult.Error && it.reason == reason }
        assertTrue(error is FieldValidationResult.Error)
        awaitButton(DescriptiveButtonState.Disabled(reason))
    }

    private suspend fun awaitButton(expected: DescriptiveButtonState) {
        assertEquals(expected, viewModel.continueButtonState.first { it == expected })
    }
}
