package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubmissionExecutor
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorStakeValidationsModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
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
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState.InputKind
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeContext
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorStakeAmountViewModelTest {

    private val resources = mock(ResourceManager::class.java)

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val token = mock(Token::class.java)

    private val assets = MutableSharedFlow<BittensorRootStakingDetails>(replay = 1)

    private val repository = object : BittensorStakingRepository {
        override fun observeRootStakingDetails() = assets

        override fun observeRootStakingDetails(metaId: Long) = assets
    }

    private val extrinsics = mock(ExtrinsicService::class.java)

    private val accounts = mock(AccountRepository::class.java)

    private val wallets = mock(WalletRepository::class.java)

    private val submission = mock(BittensorSubmissionExecutor::class.java)

    private val validators = mock(BittensorSubnetValidatorRepository::class.java)

    private val interactor = BittensorStakingInteractor(repository, extrinsics, wallets, validators, submission)

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

    private val preflightValidation = BittensorStakePreflightValidation(preflightRepository, accounts, wallets, balanceValidationSystem, subnetInteractor)

    private val validationSystem = ValidationSystem(preflightValidation)

    private val validationExecutor = ValidationExecutor()

    private val chooser = mock(AmountChooserMixin.Presentation::class.java)

    private val input = MutableStateFlow(InputState("", true, InputKind.REGULAR))

    private val chooserFactory = object : AmountChooserMixin.Factory {
        override fun create(scope: CoroutineScope, assetFlow: Flow<Asset>, maxActionProvider: MaxActionProvider?, fieldValidator: FieldValidator?) = chooser
    }

    private val feeState = MutableStateFlow<FeeStatus<BittensorFeeEstimate<BittensorRootStakeRequest>, FeeDisplay>>(FeeStatus.NoFee)

    private val feeLoader = mock(FeeLoaderMixinV2.Presentation::class.java)

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

    @Before
    fun prepare() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        `when`(resources.getString(R.string.bittensor_select_validator)).thenReturn("Choose validator")
        `when`(resources.getString(R.string.common_continue)).thenReturn("Continue")
        `when`(resources.getString(R.string.common_enter_amount)).thenReturn("Enter amount")
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("NATIVE"))
        `when`(communicator.responseFlow).thenReturn(emptyFlow())
        `when`(chooser.inputState).thenReturn(input)
        `when`(chooser.assetModel).thenReturn(emptyFlow())
        `when`(feeLoader.fee).thenReturn(feeState)
        viewModel = BittensorStakeAmountViewModel(
            resourceManager = resources,
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
            feeLoaderMixinFactory = feeLoaderFactory
        )
    }

    @After
    fun cleanUp() {
        viewModel.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `a balance refresh does not repopulate an amount cleared by the user`() = runBlocking {
        withTimeout(3_000L) {
            assets.emit(details(10))
            viewModel.amount.first { it == "5" }
            viewModel.amountChanged("")
            viewModel.amount.first { it.isEmpty() }

            assets.emit(details(11))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("10.99")) == 0 }

            assertEquals("", viewModel.amount.first())
        }
    }

    @Test
    fun `initial data preserves an amount entered before loading completes`() = runBlocking {
        withTimeout(3_000L) {
            viewModel.amountChanged("3")
            assets.emit(details(10))
            viewModel.maxAmount.first { it.toBigDecimal().compareTo(BigDecimal("9.99")) == 0 }

            assertEquals("3", viewModel.amount.first())
        }
    }

    private fun details(balance: Long): BittensorRootStakingDetails {
        val asset = mock(Asset::class.java)
        `when`(asset.token).thenReturn(token)
        `when`(asset.transferable).thenReturn(BigDecimal(balance))
        `when`(asset.transferableInPlanks).thenReturn(BigInteger.valueOf(balance).multiply(BigInteger.TEN.pow(9)))
        return BittensorRootStakingDetails(1L, chain, asset, null, null, BittensorDataStatus.Fresh)
    }
}
