package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchPriceDifferenceDescription
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSlippageDescription
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.quotedAmount
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.toSwapState
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.SwapConfirmationDetailsFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.fee.createForSwap
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.getStateOrThrow
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.setState
import io.novafoundation.nova.feature_swap_impl.presentation.main.mapSwapValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private data class SwapConfirmationState(
    val swapQuoteArgs: SwapQuoteArgs,
    val swapQuote: SwapQuote,
)

enum class MaxAction {
    ACTIVE,
    DISABLED
}

class SwapConfirmationViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val walletUiUseCase: WalletUiUseCase,
    private val slippageAlertMixinFactory: SlippageAlertMixinFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val validationExecutor: ValidationExecutor,
    private val tokenRepository: TokenRepository,
    private val externalActions: ExternalActions.Presentation,
    private val swapStateStoreProvider: SwapStateStoreProvider,
    private val feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val arbitraryAssetUseCase: ArbitraryAssetUseCase,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    private val swapConfirmationDetailsFormatter: SwapConfirmationDetailsFormatter,
    private val resourceManager: ResourceManager,
    private val swapFlowScopeAggregator: SwapFlowScopeAggregator,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val swapFlowScope = swapFlowScopeAggregator.getFlowScope(viewModelScope)

    private val confirmationStateFlow = singleReplaySharedFlow<SwapConfirmationState>()

    private val metaAccountFlow = accountRepository.selectedMetaAccountFlow()
        .shareInBackground()

    private val slippageConfigFlow = confirmationStateFlow
        .mapNotNull { swapInteractor.slippageConfig(it.swapQuote.assetIn.chainId) }
        .shareInBackground()

    private val initialSwapState = flowOf { swapStateStoreProvider.getStateOrThrow(swapFlowScope) }

    private val slippageFlow = initialSwapState.map { it.slippage }
        .shareInBackground()

    val slippageAlertMixin = slippageAlertMixinFactory.create(slippageConfigFlow, slippageFlow)

    private val chainIn = initialSwapState.map {
        chainRegistry.getChain(it.quote.assetIn.chainId)
    }
        .shareInBackground()

    private val assetInFlow = initialSwapState.flatMapLatest {
        arbitraryAssetUseCase.assetFlow(it.quote.assetIn)
    }
        .shareInBackground()

    private val assetOutFlow = initialSwapState.flatMapLatest {
        arbitraryAssetUseCase.assetFlow(it.quote.assetOut)
    }
        .shareInBackground()

    private val maxActionFlow = MutableStateFlow(MaxAction.DISABLED)

    val feeMixin = feeLoaderMixinFactory.createForSwap(
        chainAssetIn = initialSwapState.map { it.quote.assetIn },
        interactor = swapInteractor
    )

    private val maxActionProvider = createMaxActionProvider()

    private val _validationInProgress = MutableStateFlow(false)

    val validationInProgress = _validationInProgress

    val swapDetails = confirmationStateFlow.map {
        swapConfirmationDetailsFormatter.format(it.swapQuote, slippageFlow.first())
    }

    val wallet: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow()

    val addressFlow: Flow<AddressModel> = combine(chainIn, metaAccountFlow) { chainId, metaAccount ->
        addressIconGenerator.createAccountAddressModel(chainId, metaAccount)
    }

    init {
        initConfirmationState()

        handleMaxClick()
    }

    fun backClicked() {
        swapRouter.back()
    }

    fun rateClicked() {
        launchSwapRateDescription()
    }

    fun priceDifferenceClicked() {
        launchPriceDifferenceDescription()
    }

    fun slippageClicked() {
        launchSlippageDescription()
    }

    fun networkFeeClicked() = setSwapStateAndThen {
        swapRouter.openSwapFee()
    }

    fun routeClicked() = setSwapStateAfter {
        swapRouter.openSwapRoute()
    }

    fun accountClicked() {
        launch {
            val chainIn = chainIn.first()
            val addressModel = addressFlow.first()

            externalActions.showAddressActions(addressModel.address, chainIn)
        }
    }

    fun confirmButtonClicked() {
        launch {
            _validationInProgress.value = true

            val validationSystem = swapInteractor.validationSystem()
            val payload = getValidationPayload()

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                progressConsumer = _validationInProgress.progressConsumer(),
                validationFailureTransformerCustom = ::formatValidationFailure,
                block = ::executeSwap
            )
        }
    }

    private fun setSwapStateAndThen(action: () -> Unit) {
        launch {
            updateSwapStateInStore()

            action()
        }
    }

    private fun setSwapStateAfter(action: () -> Unit) {
        launch {
            val store = swapStateStoreProvider.getStore(swapFlowScope)
            store.resetState()

            action()

            updateSwapStateInStore()
        }
    }

    private suspend fun updateSwapStateInStore() {
        swapStateStoreProvider.setState(getValidationPayload().toSwapState())
    }

    private fun createMaxActionProvider(): MaxActionProvider {
        return maxActionProviderFactory.create(
            viewModelScope = viewModelScope,
            assetInFlow = assetInFlow,
            feeLoaderMixin = feeMixin,
        )
    }

    private fun executeSwap(validPayload: SwapValidationPayload) = launchUnit {
        if (swapInteractor.isDeepSwapAvailable()) {
            swapStateStoreProvider.setState(validPayload.toSwapState())

            swapRouter.openSwapExecution()
        } else {
            executeFirstSwapStep(validPayload.fee)
        }
    }

    private suspend fun executeFirstSwapStep(fee: SwapFee) {
        swapInteractor.submitFirstSwapStep(fee)
            .onSuccess {
                _validationInProgress.value = false

                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) {
                    val asset = assetOutFlow.first()
                    swapRouter.openBalanceDetails(asset.token.configuration.toAssetPayload())
                }
            }.onFailure {
                _validationInProgress.value = false

                showError(resourceManager.getString(R.string.common_undefined_error_message))
            }
    }

    private suspend fun getValidationPayload(): SwapValidationPayload {
        val confirmationState = confirmationStateFlow.first()
        val swapFee = feeMixin.awaitFee()

        return SwapValidationPayload(
            swapQuote = confirmationState.swapQuote,
            fee = swapFee,
            slippage = slippageFlow.first()
        )
    }

    private fun formatValidationFailure(
        status: ValidationStatus.NotValid<SwapValidationFailure>,
        actions: ValidationFlowActions<SwapValidationPayload>
    ): TransformedFailure {
        return mapSwapValidationFailureToUI(
            resourceManager,
            status,
            actions,
            amountInSwapMaxAction = ::setMaxAmountIn,
            amountOutSwapMinAction = { _, amount -> setMinAmountOut(amount) }
        )
    }

    private fun setMaxAmountIn() {
        launch {
            maxActionFlow.value = MaxAction.ACTIVE
        }
    }

    private fun setMinAmountOut(amount: Balance) = launchUnit {
        maxActionFlow.value = MaxAction.DISABLED

        val confirmationState = confirmationStateFlow.first()

        calculateQuote(
            confirmationState.swapQuoteArgs.copy(
                amount = amount,
                swapDirection = SwapDirection.SPECIFIED_OUT
            )
        )
    }

    private fun calculateQuote(newSwapQuoteArgs: SwapQuoteArgs) {
        launch {
            val confirmationState = confirmationStateFlow.first()
            val swapQuote = swapInteractor.quote(newSwapQuoteArgs, swapFlowScope)
                .onFailure { }
                .getOrNull() ?: return@launch

            feeMixin.loadFee { feePaymentCurrency ->
                val executeArgs = swapQuote.toExecuteArgs(
                    slippage = slippageFlow.first(),
                    firstSegmentFees = feePaymentCurrency
                )

                swapInteractor.estimateFee(executeArgs)
            }

            val newState = confirmationState.copy(swapQuoteArgs = newSwapQuoteArgs, swapQuote = swapQuote)
            confirmationStateFlow.emit(newState)
        }
    }

    private fun initConfirmationState() {
        launch {
            val swapState = initialSwapState.first()

            val swapQuote = swapState.quote

            val assetIn = swapQuote.assetIn
            val assetOut = swapQuote.assetOut

            val quoteArgs = SwapQuoteArgs(
                tokenIn = tokenRepository.getToken(assetIn),
                tokenOut = tokenRepository.getToken(assetOut),
                amount = swapQuote.quotedPath.quotedAmount,
                swapDirection = swapQuote.quotedPath.direction,
            )

            feeMixin.setFee(swapState.fee)

            val newState = SwapConfirmationState(quoteArgs, swapQuote)
            confirmationStateFlow.emit(newState)
        }
    }

    private fun handleMaxClick() {
        combineToPair(maxActionFlow, maxActionProvider.maxAvailableBalance)
            .filter { (maxAction, _) -> maxAction == MaxAction.ACTIVE }
            .mapNotNull { it.second.actualBalance }
            .distinctUntilChanged()
            .onEach {
                val confirmationState = confirmationStateFlow.first()
                calculateQuote(
                    confirmationState.swapQuoteArgs.copy(
                        amount = it,
                        swapDirection = SwapDirection.SPECIFIED_IN
                    )
                )
            }
            .launchIn(viewModelScope)
    }
}
