package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPresetValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetFacts
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetFactsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakeDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorPreset
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWalletContext
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.RecordingBittensorAnalytics
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleCostBasisFormatter
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountFieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.BaseAmountChooserProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeContext
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
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
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorSubnetAmountValidationTest {

    private val analytics = RecordingBittensorAnalytics()

    private val resources = Mockito.mock(ResourceManager::class.java)

    private val logger = Mockito.mock(Logger::class.java)

    private val interactionGate = Mockito.mock(AutomaticInteractionGate::class.java)

    private val retries = BittensorRetryController(resources, logger, interactionGate)

    private val router = Mockito.mock(StakingRouter::class.java)

    private val subnetInteractor = Mockito.mock(BittensorSubnetInteractor::class.java)

    private val stakingInteractor = Mockito.mock(BittensorSubnetStakingInteractor::class.java)

    private val presets = Mockito.mock(BittensorPresetValidatorInteractor::class.java)

    private val quickPick = Mockito.mock(BittensorQuickPickInteractor::class.java)

    private val tokenFormatter = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence = amount.toPlainString()
    }

    private val fiatFormatter = Mockito.mock(FiatFormatter::class.java)

    private val amountFormatter = BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    private val costBasisFormatter = Mockito.mock(BittensorSaleCostBasisFormatter::class.java)

    private val estimates = Mockito.mock(BittensorEstimatesMixin::class.java)

    private val slippage = Mockito.mock(BittensorSlippageSettings::class.java)

    private val slippageCommunicator = Mockito.mock(SlippageSettingsCommunicator::class.java)

    private val validatorCommunicator = Mockito.mock(BittensorSelectValidatorCommunicator::class.java)

    private val facts = Mockito.mock(BittensorSubnetFactsInteractor::class.java)

    private val icons = Mockito.mock(AddressIconGenerator::class.java)

    private val preflightValidation = Mockito.mock(BittensorStakePreflightValidation::class.java)

    private val validationSystem = ValidationSystem(preflightValidation)

    private val validationExecutor = ValidationExecutor()

    private val chain = Mockito.mock(Chain::class.java)

    private val chainAsset = Mockito.mock(Chain.Asset::class.java)

    private val token = Mockito.mock(Token::class.java)

    private val wallets = MutableStateFlow<BittensorWalletContext?>(null)

    private val fieldValidation = MutableStateFlow<FieldValidationResult>(FieldValidationResult.Ok)

    private val validator = object : EnoughAmountFieldValidator {
        override fun observe(inputStream: Flow<String>) = fieldValidation
    }

    private lateinit var validationMax: MaxActionProvider

    private val validatorFactory = object : EnoughAmountValidatorFactory {
        override fun create(maxAvailableProvider: MaxActionProvider, errorMessageRes: Int): EnoughAmountFieldValidator {
            validationMax = maxAvailableProvider
            return validator
        }
    }

    private val chooser = Mockito.mock(AmountChooserMixin.Presentation::class.java)

    private val chooserFactory = object : AmountChooserMixin.Factory {
        override fun create(
            scope: CoroutineScope,
            assetFlow: Flow<Asset>,
            maxActionProvider: MaxActionProvider?,
            fieldValidator: FieldValidator?
        ): AmountChooserMixin.Presentation {
            val base = BaseAmountChooserProvider(scope, emptyFlow(), maxActionProvider, fieldValidator = fieldValidator)
            Mockito.`when`(chooser.inputState).thenReturn(base.inputState)
            Mockito.`when`(chooser.maxAction).thenReturn(base.maxAction)
            Mockito.`when`(chooser.fieldError).thenReturn(base.fieldError)
            Mockito.`when`(chooser.assetModel).thenReturn(emptyFlow())
            return chooser
        }
    }

    private val fee = Mockito.mock(SubmissionFee::class.java)

    private val feeDraft = Mockito.mock(BittensorSubnetStakeDraft::class.java)

    private val feeEstimate = BittensorFeeEstimate(feeDraft, fee)

    private val feeState = MutableStateFlow<FeeStatus<BittensorFeeEstimate<BittensorSubnetStakeDraft>, FeeDisplay>>(
        FeeStatus.Loaded(FeeModel(feeEstimate, FeeDisplay("network fee", null)))
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

    private val getAssetOptions = Mockito.mock(GetAssetOptionsMixin::class.java)

    private val getAssetFactory = object : GetAssetOptionsMixin.Factory {
        override fun create(
            assetFlow: Flow<Chain.Asset?>,
            scope: CoroutineScope,
            additionalButtonFilter: Flow<Boolean>,
            showWhenNoOptions: Boolean
        ) = getAssetOptions
    }

    private val subnet = BittensorSubnet(BittensorNetuid.Subnet(1), "Subnet", "ALPHA", null)

    private lateinit var viewModel: BittensorSubnetAmountViewModel

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        Mockito.`when`(resources.getString(R.string.common_continue)).thenReturn("Continue")
        Mockito.`when`(resources.getString(R.string.common_enter_amount)).thenReturn("Enter amount")
        Mockito.`when`(resources.getString(R.string.bittensor_select_validator_title)).thenReturn("Select validator")
        Mockito.`when`(token.configuration).thenReturn(chainAsset)
        Mockito.`when`(chainAsset.precision).thenReturn(Precision(9))
        Mockito.`when`(chainAsset.symbol).thenReturn(TokenSymbol("TAO"))
        Mockito.`when`(chainAsset.chainId).thenReturn("bittensor")
        Mockito.`when`(fee.maxAmountDeductionFor(chainAsset)).thenReturn(BigInteger("1040000"))
        Mockito.`when`(feeLoader.fee).thenReturn(feeState)
        Mockito.`when`(subnetInteractor.observeWallet()).thenReturn(wallets.filterNotNull())
        Mockito.`when`(subnetInteractor.observeSubnetLogos()).thenReturn(flowOf(emptyMap()))
        Mockito.`when`(subnetInteractor.subnet(subnet.id)).thenReturn(subnet)
        Mockito.`when`(presets.preset(subnet.id)).thenReturn(BittensorValidatorPreset(emptyList(), null))
        Mockito.`when`(slippage.observe()).thenReturn(flowOf(BigDecimal("0.005")))
        Mockito.`when`(slippageCommunicator.responseFlow).thenReturn(emptyFlow())
        Mockito.`when`(validatorCommunicator.responseFlow).thenReturn(emptyFlow())
        Mockito.`when`(facts.facts(subnet)).thenReturn(BittensorSubnetFacts(null, deepPool = false, steady = false))
        Mockito.`when`(stakingInteractor.requotes(Mockito.any<Flow<Boolean>>() ?: flowOf(true), Mockito.any<Flow<Unit>>() ?: emptyFlow()))
            .thenReturn(emptyFlow())
        Mockito.`when`(estimates.observePositionCostBasis(Mockito.any<Flow<BittensorEstimateContext?>>() ?: emptyFlow()))
            .thenReturn(emptyFlow())
        Mockito.`when`(estimates.observeAlphaYields(Mockito.any<Flow<Chain?>>() ?: emptyFlow(), Mockito.eq(subnet.id) ?: subnet.id))
            .thenReturn(flowOf(ExtendedLoadingState.Loaded(emptyMap())))
        viewModel = createViewModel()
    }

    private fun createViewModel() = BittensorSubnetAmountViewModel(
        analytics = analytics,
        initialSubnetId = subnet.id,
        quickPickOrder = null,
        quickPickInteractor = quickPick,
        presetValidatorInteractor = presets,
        validatorAddress = null,
        validatorIsPreset = false,
        recommendedAddress = null,
        bittensorSubnetInteractor = subnetInteractor,
        router = router,
        resources = resources,
        tokenFormatter = tokenFormatter,
        logger = logger,
        bittensorSubnetStakingInteractor = stakingInteractor,
        fiatFormatter = fiatFormatter,
        retryController = retries,
        bittensorAmountFormatter = amountFormatter,
        amountChooserFactory = chooserFactory,
        enoughAmountValidatorFactory = validatorFactory,
        validationSystem = validationSystem,
        validationExecutor = validationExecutor,
        feeLoaderMixinFactory = feeFactory,
        existingPosition = false,
        costBasisFormatter = costBasisFormatter,
        estimatesMixin = estimates,
        slippageSettings = slippage,
        slippageSettingsCommunicator = slippageCommunicator,
        validatorCommunicator = validatorCommunicator,
        getAssetOptionsMixinFactory = getAssetFactory,
        subnetFactsInteractor = facts,
        iconGenerator = icons
    )

    @After
    fun tearDown() = runBlocking<Unit> {
        if (::viewModel.isInitialized) viewModel.coroutineContext.job.cancelAndJoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `validation receives Max after reserve and fee and a field error blocks validator selection`() = runBlocking<Unit> {
        withTimeout(3_000) {
            wallets.value = wallet(BigInteger("60490000"))
            val maximum = validationMax.maxAvailableBalance.first { it.actualBalance == BigInteger("49450000") }
            assertEquals(BigInteger("49450000"), maximum.displayedBalance)
            viewModel.amountChanged("50.03")
            fieldValidation.value = insufficientBalance()
            assertTrue(viewModel.amountFieldError.first { it is FieldValidationResult.Error } is FieldValidationResult.Error)
            assertEquals(DescriptiveButtonState.Disabled("Not enough tokens"), viewModel.continueButtonState.first {
                it == DescriptiveButtonState.Disabled("Not enough tokens")
            })
            assertEquals("50.03", viewModel.amount.first())
            viewModel.continueClicked()
            Mockito.verify(validatorCommunicator, Mockito.never()).openRequest(
                Mockito.any(BittensorSelectValidatorCommunicator.Request::class.java)
                    ?: BittensorSelectValidatorCommunicator.Request(subnet.id.value, null)
            )

            viewModel.amountChanged("0.04945")
            fieldValidation.value = FieldValidationResult.Ok
            assertEquals(DescriptiveButtonState.Enabled("Select validator"), viewModel.continueButtonState.first {
                it is DescriptiveButtonState.Enabled
            })
        }
    }

    @Test
    fun `positive input on an empty wallet explains insufficient balance ahead of funding state`() = runBlocking<Unit> {
        withTimeout(3_000) {
            wallets.value = wallet(BigInteger.ZERO)
            viewModel.needsFunding.first { it }
            viewModel.amountChanged("1")
            fieldValidation.value = insufficientBalance()
            assertEquals(DescriptiveButtonState.Disabled("Not enough tokens"), viewModel.continueButtonState.first {
                it == DescriptiveButtonState.Disabled("Not enough tokens")
            })
            viewModel.amountChanged("")
            fieldValidation.value = FieldValidationResult.Ok
            assertEquals(DescriptiveButtonState.Disabled("Enter amount"), viewModel.continueButtonState.first {
                it == DescriptiveButtonState.Disabled("Enter amount")
            })
        }
    }

    private fun insufficientBalance() = FieldValidationResult.Error("Not enough tokens", EnoughAmountFieldValidator.ERROR_TAG)

    private fun wallet(balance: BigInteger): BittensorWalletContext {
        val asset = Mockito.mock(Asset::class.java)
        Mockito.`when`(asset.token).thenReturn(token)
        Mockito.`when`(asset.transferableInPlanks).thenReturn(balance)
        Mockito.`when`(asset.transferable).thenReturn(BigDecimal(balance, 9))
        return BittensorWalletContext(1L, chain, asset)
    }
}
