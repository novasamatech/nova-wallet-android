package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import org.mockito.ArgumentCaptor
import org.junit.Assert.assertNotEquals
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectionState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetBrowseState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorReplaceOrigin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.RecordingBittensorAnalytics
import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorStakeValidationsModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorImmediateExecution
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorImmediateStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubmissionExecutor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.BaseAmountChooserProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeContext
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorStakeAmountViewModelTest {

    private val analyticsService = RecordingBittensorAnalytics()
    private val analytics = analyticsService

    private val resources = mock(ResourceManager::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val token = mock(Token::class.java)

    private val assets = MutableSharedFlow<BittensorRootStakingDetails>(replay = 1)

    private val explicitAssets = MutableSharedFlow<BittensorRootStakingDetails>(replay = 1)

    private val explicitWallets = mutableListOf<Long>()

    private var rawBalanceLoads = 0

    private val repository = object : BittensorStakingRepository {
        override fun observeRootStakingDetails() = assets

        override fun observeRootStakingDetails(metaId: Long): Flow<BittensorRootStakingDetails> {
            explicitWallets += metaId
            return explicitAssets
        }

        override fun observeRootAccountDetails(metaId: Long): Flow<BittensorRootStakingDetails> = flow {
            explicitWallets += metaId
            rawBalanceLoads++
            emitAll(explicitAssets)
        }
    }

    private val extrinsics = mock(ExtrinsicService::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val submission = mock(BittensorSubmissionExecutor::class.java)

    private val validators = mock(BittensorSubnetValidatorRepository::class.java)

    private val interactor = BittensorStakingInteractor(
        repository,
        extrinsics,
        wallets,
        validators,
        submission
    )

    private val router = mock(StakingRouter::class.java)

    private val addressIcons = mock(AddressIconGenerator::class.java)

    private val communicator = mock(BittensorSelectValidatorCommunicator::class.java)

    private val fiatFormatter = mock(FiatFormatter::class.java)

    private val tokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence = amount.toPlainString()
    }

    private val logger = mock(Logger::class.java)

    private val retryController = BittensorRetryController(resources, logger)

    private val amountFormatter = BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    private val preflightRepository = mock(BittensorStakePreflightRepository::class.java)

    private val bittensorModule = BittensorStakeValidationsModule()

    private val balanceValidationSystem = bittensorModule.provideStakeValidationSystem()

    private val subnetInteractor = mock(BittensorSubnetInteractor::class.java)

    private val immediateExecution = mock(BittensorImmediateExecution::class.java)

    private val immediateValidation = BittensorImmediateStakeValidation()

    private val preflightValidation = BittensorStakePreflightValidation(
        preflightRepository,
        accounts,
        wallets,
        balanceValidationSystem,
        subnetInteractor,
        validators,
        immediateExecution,
        immediateValidation
    )

    private val validationSystem = ValidationSystem(preflightValidation)

    private val validationExecutor = ValidationExecutor()

    private val chooser = mock(AmountChooserMixin.Presentation::class.java)

    private val chooserFactory = object : AmountChooserMixin.Factory {
        override fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            maxActionProvider: MaxActionProvider?,
            fieldValidator: FieldValidator?
        ): AmountChooserMixin.Presentation {
            val base = BaseAmountChooserProvider(scope, emptyFlow(), maxActionProvider)
            `when`(chooser.inputState).thenReturn(base.inputState)
            `when`(chooser.maxAction).thenReturn(base.maxAction)
            return chooser
        }
    }

    private val feeState = MutableStateFlow<FeeStatus<BittensorFeeEstimate<BittensorRootStakeRequest>, FeeDisplay>>(FeeStatus.NoFee)

    private val feeLoader = mock(FeeLoaderMixinV2.Presentation::class.java)

    private val estimateFee = mock(SubmissionFee::class.java)

    private val estimateValidator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), "Validator")

    private val estimateIcon = mock(Drawable::class.java)

    private val unusedFeeLoaderFactory = mock(FeeLoaderMixinV2.Factory::class.java)

    private val feeLoaderFactory = object : FeeLoaderMixinV2.Factory by unusedFeeLoaderFactory {
        @Suppress("UNCHECKED_CAST")
        override fun <F : SubmissionFee> createDefault(
            scope: CoroutineScope,
            feeContextFlow: Flow<FeeContext>,
            configuration: FeeLoaderMixinV2.Configuration<F, FeeDisplay>
        ): FeeLoaderMixinV2.Presentation<F, FeeDisplay> = feeLoader as FeeLoaderMixinV2.Presentation<F, FeeDisplay>
    }

    private lateinit var viewModel: BittensorStakeAmountViewModel

    private val getAssetOptions = mock(GetAssetOptionsMixin::class.java)

    private val getAssetOptionsFactory = object : GetAssetOptionsMixin.Factory {
        override fun create(
            assetFlow: Flow<Chain.Asset?>,
            scope: CoroutineScope,
            additionalButtonFilter: Flow<Boolean>,
            showWhenNoOptions: Boolean
        ) = getAssetOptions
    }

    @Before
    fun prepare() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(resources.getString(R.string.bittensor_select_validator)).thenReturn("Choose validator")
        `when`(resources.getString(R.string.common_continue)).thenReturn("Continue")
        `when`(resources.getString(R.string.common_enter_amount)).thenReturn("Enter amount")
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Error")
        `when`(resources.getString(R.string.bittensor_load_failed_message)).thenReturn("Retry")
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.chainId).thenReturn("bittensor")
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("NATIVE"))
        `when`(estimateFee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger.ZERO)
        `when`(communicator.responseFlow).thenReturn(emptyFlow())
        `when`(chooser.assetModel).thenReturn(emptyFlow())
        `when`(feeLoader.fee).thenReturn(feeState)
        runBlocking {
            `when`(
                addressIcons.createAddressIcon(estimateValidator.hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT)
            )
                .thenReturn(estimateIcon)
        }
        createViewModel()
    }

    private fun createViewModel(
        payload: BittensorStakeAmountPayload? = null,
        presentation: BittensorRootStakePresentation = BittensorRootStakePresentation.STANDARD,
        selectionState: BittensorSelectionState = BittensorSelectionState()
    ) {
        if (::viewModel.isInitialized) {
            runBlocking { viewModel.coroutineContext.job.cancelAndJoin() }
        }
        val externalActions = mock(ExternalActions.Presentation::class.java)
        viewModel = BittensorStakeAmountViewModel(
            analytics, resourceManager = resources,
            router = router,
            bittensorStakingInteractor = interactor,
            iconGenerator = addressIcons,
            validatorCommunicator = communicator,
            fiatFormatter = fiatFormatter,
            tokenFormatter = tokenFormatter,
            retryController = retryController,
            bittensorAmountFormatter = amountFormatter,
            amountChooserFactory = chooserFactory,
            stakeValidationSystem = validationSystem,
            validationExecutor = validationExecutor,
            feeLoaderMixinFactory = feeLoaderFactory,
            externalActions = externalActions,
            getAssetOptionsMixinFactory = getAssetOptionsFactory,
            payload = payload,
            initialSelectionState = selectionState,
            presentation = presentation
        )
    }

    @After
    fun cleanUp() = runBlocking<Unit> {
        if (::viewModel.isInitialized) viewModel.coroutineContext.job.cancelAndJoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `selected max follows late fee while manual edits stop following`() = runBlocking<Unit> {
        withTimeout(3_000L) {
            val lowBalance = details(1)
            `when`(lowBalance.asset.transferableInPlanks).thenReturn(BigInteger("10010000"))
            assets.emit(lowBalance)
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("0.00001")) == 0 }
            viewModel.maxClicked()
            viewModel.amount.first { it == "0.00001" }
            // The view's text listener also observes programmatic updates.
            viewModel.amountChanged("0.00001")
            assertEquals(InputKind.MAX_ACTION, chooser.inputState.value.inputKind)

            `when`(estimateFee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger("930000"))
            val request = BittensorRootStakeRequest(1L, chain, estimateValidator, BigInteger.ONE)
            val estimate = BittensorFeeEstimate(request, estimateFee)
            val display = FeeDisplay("network fee", null)
            feeState.value = FeeStatus.Loaded(FeeModel(estimate, display))
            viewModel.amount.first { it == "0" }

            viewModel.amountChanged("2")
            assets.emit(details(11))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("10.98907")) == 0 }
            assertEquals("2", chooser.inputState.value.value)
            assertEquals(InputKind.REGULAR, chooser.inputState.value.inputKind)
        }
    }

    @Test
    fun `max label and input use the same loaded fee and retain it during refresh`() = runBlocking<Unit> {
        withTimeout(3_000L) {
            assets.emit(details(10))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("9.99")) == 0 }
            `when`(estimateFee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger("930000"))
            val request = BittensorRootStakeRequest(1L, chain, estimateValidator, BigInteger.TEN.pow(9))
            val estimate = BittensorFeeEstimate(request, estimateFee)
            val display = FeeDisplay("network fee", null)
            feeState.value = FeeStatus.Loaded(FeeModel(estimate, display))
            val maximum = viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("9.98907")) == 0 }

            viewModel.maxClicked()
            val entered = viewModel.amount.first { it == "9.98907" }
            assertEquals(0, maximum.toBigDecimal().compareTo(entered.toBigDecimal()))

            feeState.value = FeeStatus.Loading(visibleDuringProgress = true)
            viewModel.amountChanged("1")
            viewModel.maxClicked()
            val refreshed = viewModel.amount.first { it == "9.98907" }
            assertEquals(0, maximum.toBigDecimal().compareTo(refreshed.toBigDecimal()))
        }
    }

    @Test
    fun `max clamps to zero when fee and reserve exceed transferable balance`() = runBlocking<Unit> {
        withTimeout(3_000L) {
            val details = details(1)
            `when`(details.asset.transferableInPlanks).thenReturn(BigInteger("10010000"))
            assets.emit(details)
            `when`(estimateFee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger("930000"))
            val request = BittensorRootStakeRequest(1L, chain, estimateValidator, BigInteger.ONE)
            val estimate = BittensorFeeEstimate(request, estimateFee)
            val display = FeeDisplay("network fee", null)
            feeState.value = FeeStatus.Loaded(FeeModel(estimate, display))

            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal.ZERO) == 0 }
            viewModel.maxClicked()
            assertEquals("0", viewModel.amount.first { it == "0" })
        }
    }

    @Test
    fun `add stake caption requires an existing positive stake`() = runBlocking<Unit> {
        createViewModel(BittensorStakeAmountPayload(1L, null))
        val position = BittensorRootPosition(estimateValidator.hotkey, BigInteger("10000000"))
        val details = details(10).copy(validator = estimateValidator, position = position)
        `when`(resources.getString(R.string.bittensor_adds_to_position, "0.010000000", "Validator"))
            .thenReturn("Adds to your 0.01 staked with Validator")

        withTimeout(3_000L) {
            explicitAssets.emit(details)
            assertEquals("Adds to your 0.01 staked with Validator", viewModel.addStakeCaption.first { it != null })
            explicitAssets.emit(details.copy(position = position.copy(stakeInPlanks = BigInteger.ZERO)))
            assertEquals(null, viewModel.addStakeCaption.first { it == null })
        }
    }

    @Test
    fun `fresh staking does not show the add stake caption`() = runBlocking<Unit> {
        withTimeout(3_000L) {
            val position = BittensorRootPosition(estimateValidator.hotkey, BigInteger("10000000"))
            assets.emit(details(10).copy(validator = estimateValidator, position = position))
            viewModel.amountChanged("5")
            viewModel.amount.first { it == "5" }
            assertEquals(null, viewModel.addStakeCaption.first())
        }
    }

    @Test
    fun `zero input retains a placeholder fee without enabling continue`() = runBlocking<Unit> {
        withTimeout(3_000L) {
            assets.emit(details(10).copy(validator = estimateValidator))
            viewModel.amountChanged("5")
            viewModel.amount.first { it == "5" }
            viewModel.amountChanged("0")
            val request = BittensorRootStakeRequest(1L, chain, estimateValidator, BigInteger.TEN.pow(9))
            val estimate = BittensorFeeEstimate(request, estimateFee)
            val display = FeeDisplay("network fee", null)
            feeState.value = FeeStatus.Loaded(FeeModel(estimate, display))

            viewModel.fee.first { it is FeeStatus.Loaded }
            assertEquals("0", viewModel.amount.first())
            assertTrue(viewModel.continueButtonState.first { it is DescriptiveButtonState.Disabled } is DescriptiveButtonState.Disabled)
        }
    }

    @Test
    fun `cleared input retains a placeholder fee and a new amount invalidates it`() = runBlocking<Unit> {
        withTimeout(3_000L) {
            assets.emit(details(10).copy(validator = estimateValidator))
            viewModel.amountChanged("5")
            viewModel.amount.first { it == "5" }
            viewModel.amountChanged("")
            val request = BittensorRootStakeRequest(1L, chain, estimateValidator, BigInteger.TEN.pow(9))
            val estimate = BittensorFeeEstimate(request, estimateFee)
            val display = FeeDisplay("network fee", null)
            feeState.value = FeeStatus.Loaded(FeeModel(estimate, display))

            viewModel.fee.first { it is FeeStatus.Loaded }
            assertEquals("", viewModel.amount.first())
            viewModel.continueButtonState.first { it is DescriptiveButtonState.Disabled }

            viewModel.amountChanged("2")
            assertTrue(viewModel.fee.first { it is FeeStatus.Loading } is FeeStatus.Loading)
        }
    }

    @Test
    fun `setup opens with an empty amount even when the balance is below the old suggestion`() = runBlocking {
        withTimeout(3_000L) {
            assets.emit(details(1))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("0.99")) == 0 }

            assertEquals("", viewModel.amount.first())
        }
    }

    @Test
    fun `add stake opens with an empty amount instead of the full transferable balance`() = runBlocking {
        createViewModel(BittensorStakeAmountPayload(1L, null))
        withTimeout(3_000L) {
            explicitAssets.emit(details(1))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("0.99")) == 0 }

            assertEquals("", viewModel.amount.first())
        }
    }

    @Test
    fun `a balance refresh does not repopulate an amount cleared by the user`() = runBlocking {
        withTimeout(3_000L) {
            assets.emit(details(10))
            viewModel.amountChanged("5")
            viewModel.amount.first { it == "5" }
            viewModel.amountChanged("")
            viewModel.amount.first { it.isEmpty() }

            assets.emit(details(11))
            viewModel.maxAmount.first {
                it.toBigDecimal().compareTo(BigDecimal("10.99")) == 0
            }

            assertEquals("", viewModel.amount.first())
        }
    }

    @Test
    fun `initial data preserves an amount entered before loading completes`() = runBlocking {
        withTimeout(3_000L) {
            viewModel.amountChanged("3")
            assets.emit(details(10))
            viewModel.maxAmount.first {
                it.toBigDecimal().compareTo(BigDecimal("9.99")) == 0
            }

            assertEquals("3", viewModel.amount.first())
        }
    }

    @Test
    fun `rewards only payload stays on explicit wallet when globally selected wallet changes`() = runBlocking<Unit> {
        createViewModel(BittensorStakeAmountPayload(1L, null))
        withTimeout(3_000L) {
            explicitAssets.emit(details(10))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("9.99")) == 0 }
            assets.emit(details(100).copy(metaId = 2L))
            explicitAssets.emit(details(11))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("10.99")) == 0 }
            assertEquals(listOf(1L), explicitWallets)
            assertEquals(getAssetOptions, viewModel.getAssetOptionsMixin)
        }
    }

    @Test
    fun `fixed validator retry preserves reactive balances and resolves only once per attempt`() = runBlocking<Unit> {
        `when`(chain.addressPrefix).thenReturn(42)
        val hotkey = AccountIdKey(ByteArray(32) { 3 })
        val fixed = BittensorValidator(hotkey, "Fixed validator")
        val address = chain.addressOf(hotkey)
        val candidate = BittensorSubnetValidator(fixed, address, BigDecimal.ZERO, selectable = true)
        val icon = mock(Drawable::class.java)
        `when`(addressIcons.createAddressIcon(hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT))
            .thenReturn(icon)
        `when`(validators.rootValidators(chain))
            .thenThrow(IllegalStateException("Eligibility unavailable"))
            .thenReturn(listOf(candidate))
        createViewModel(BittensorStakeAmountPayload(1L, address))

        withTimeout(3_000L) {
            explicitAssets.emit(details(10))
            val retry = viewModel.retryEvents.first()
            viewModel.continueButtonState.first { it is DescriptiveButtonState.Disabled }
            viewModel.amountChanged("3")
            explicitAssets.emit(details(11))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("10.99")) == 0 }
            assertEquals(1, rawBalanceLoads)
            verify(validators, times(1)).rootValidators(chain)

            retry.onRetry()
            viewModel.validator.first { it.name == "Fixed validator" }
            explicitAssets.emit(details(12))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("11.99")) == 0 }
            assertEquals("3", viewModel.amount.first())
            assertEquals(1, rawBalanceLoads)
            verify(validators, times(2)).rootValidators(chain)
            assertEquals(getAssetOptions, viewModel.getAssetOptionsMixin)
        }
    }

    @Test
    fun `empty wallet shows funding then restores amount after replenishment`() = runBlocking {
        withTimeout(3_000L) {
            assets.emit(details(0))
            viewModel.needsFunding.first { it }
            assets.emit(details(10))
            viewModel.needsFunding.first { !it }
            assertEquals(getAssetOptions, viewModel.getAssetOptionsMixin)
        }
    }

    private fun details(balance: Long): BittensorRootStakingDetails {
        val asset = mock(Asset::class.java)
        `when`(asset.token).thenReturn(token)
        `when`(asset.transferable).thenReturn(BigDecimal(balance))
        `when`(asset.transferableInPlanks).thenReturn(
            BigInteger.valueOf(balance)
                .multiply(BigInteger.TEN.pow(9))
        )
        return BittensorRootStakingDetails(
            1L,
            chain,
            asset,
            null,
            null,
            BittensorDataStatus.Fresh
        )
    }

    @Test
    fun `title follows the root entry`() {
        createViewModel(presentation = BittensorRootStakePresentation.QUICK_PICK)
        assertEquals(R.string.bittensor_earn_title, viewModel.titleRes)

        createViewModel()
        assertEquals(R.string.bittensor_stake_to_root, viewModel.titleRes)

        createViewModel(BittensorStakeAmountPayload(1L, "validator"), BittensorRootStakePresentation.QUICK_PICK)
        assertEquals(R.string.bittensor_add_stake_to_root, viewModel.titleRes)
    }

    @Test
    fun `quick pick header opens the plain root setup under its own flow`() {
        val state = BittensorSelectionState(browseState = BittensorSubnetBrowseState(hideThinPools = true))
        createViewModel(presentation = BittensorRootStakePresentation.QUICK_PICK, selectionState = state)

        viewModel.quickPickHeaderClicked()

        val captor = ArgumentCaptor.forClass(BittensorSelectionState::class.java)
        verify(router).openBittensorRootStaking(
            org.mockito.ArgumentMatchers.isNull(),
            captor.capture() ?: state,
            org.mockito.ArgumentMatchers.eq(BittensorRootStakePresentation.STANDARD) ?: BittensorRootStakePresentation.STANDARD
        )
        assertNotEquals(state.flowId, captor.value.flowId)
        assertEquals(state.browseState, captor.value.browseState)
    }

    @Test
    fun `see all subnets from root replaces the root setup and add stake has none`() {
        createViewModel()
        viewModel.seeAllSubnetsClicked()

        val captor = ArgumentCaptor.forClass(BittensorChooseSubnetPayload::class.java)
        verify(router).openBittensorChooseSubnet(captor.capture() ?: BittensorChooseSubnetPayload())
        assertEquals(true, captor.value.replaceAmount)
        assertEquals(BittensorReplaceOrigin.ROOT_STAKE, captor.value.replaceOrigin)

        createViewModel(BittensorStakeAmountPayload(1L, "validator"))
        viewModel.seeAllSubnetsClicked()
        verify(router, org.mockito.Mockito.times(1)).openBittensorChooseSubnet(org.mockito.ArgumentMatchers.any() ?: BittensorChooseSubnetPayload())
    }
}
