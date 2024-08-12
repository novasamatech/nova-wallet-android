package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.asPercent
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.description.launchNetworkFeeDescription
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.icon
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.editedBalance
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.totalDeductedPlanks
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.view.SwapAssetView
import io.novafoundation.nova.feature_swap_api.presentation.view.SwapAssetsView
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.model.SwapConfirmationDetailsModel
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayloadFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.main.mapSwapValidationFailureToUI
import io.novafoundation.nova.feature_swap_impl.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.getDecimalFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

private data class SwapConfirmationState(
    val swapQuoteArgs: SwapQuoteArgs,
    val swapQuote: SwapQuote,
    val feeAsset: Chain.Asset
)

enum class MaxAction {
    ACTIVE,
    DISABLED
}

class SwapConfirmationViewModel(
    private val swapRouter: SwapRouter,
    private val swapInteractor: SwapInteractor,
    private val resourceManager: ResourceManager,
    private val payload: SwapConfirmationPayload,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val swapRateFormatter: SwapRateFormatter,
    private val priceImpactFormatter: PriceImpactFormatter,
    private val walletUiUseCase: WalletUiUseCase,
    private val slippageAlertMixinFactory: SlippageAlertMixinFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val validationExecutor: ValidationExecutor,
    private val tokenRepository: TokenRepository,
    private val externalActions: ExternalActions.Presentation,
    private val swapConfirmationPayloadFormatter: SwapConfirmationPayloadFormatter,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val arbitraryAssetUseCase: ArbitraryAssetUseCase,
    private val maxActionProviderFactory: MaxActionProviderFactory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher {

    private val confirmationStateFlow = MutableStateFlow<SwapConfirmationState?>(null)

    private val metaAccountFlow = accountRepository.selectedMetaAccountFlow()
        .shareInBackground()

    private val slippageConfigFlow = confirmationStateFlow
        .filterNotNull()
        .mapNotNull { swapInteractor.slippageConfig(it.swapQuote.assetIn.chainId) }
        .shareInBackground()

    private val slippageFlow = flowOf { payload.slippage.asPercent() }
        .shareInBackground()

    private val slippageAlertMixin = slippageAlertMixinFactory.create(slippageConfigFlow, slippageFlow)

    private val chainWithAssetFlow = flowOf {
        val assetIn = payload.swapQuoteModel.assetIn
        chainRegistry.chainWithAsset(assetIn.chainId, assetIn.chainAssetId)
    }
        .shareInBackground()

    private val assetInFlow = arbitraryAssetUseCase.assetFlow(
        payload.swapQuoteModel.assetIn.chainId,
        payload.swapQuoteModel.assetIn.chainAssetId
    )
        .shareInBackground()

    private val assetOutFlow = arbitraryAssetUseCase.assetFlow(
        payload.swapQuoteModel.assetOut.chainId,
        payload.swapQuoteModel.assetOut.chainAssetId
    )
        .shareInBackground()

    private val maxActionFlow = MutableStateFlow(MaxAction.DISABLED)

    private val feeTokenFlow = arbitraryAssetUseCase.assetFlow(payload.feeAsset.chainId, payload.feeAsset.chainAssetId)
        .map { it.token }
        .shareInBackground()

    val feeMixin = feeLoaderMixinFactory.createGeneric<SwapFee>(
        tokenFlow = feeTokenFlow,
        configuration = GenericFeeLoaderMixin.Configuration(
            initialStatusValue = FeeStatus.Loading,
        )
    )

    private val maxActionProvider = createMaxActionProvider()

    private val _validationProgress = MutableStateFlow(false)

    val validationProgress = _validationProgress

    val swapDetails = confirmationStateFlow.filterNotNull().map {
        formatToSwapDetailsModel(it)
    }

    val wallet: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow()

    val addressFlow: Flow<AddressModel> = combine(chainWithAssetFlow, metaAccountFlow) { chainWithAsset, metaAccount ->
        addressIconGenerator.createAccountAddressModel(chainWithAsset.chain, metaAccount)
    }

    val slippageAlertMessage: Flow<String?> = slippageAlertMixin.slippageAlertMessage

    init {
        handleMaxClick()

        initConfirmationState()
    }

    fun backClicked() {
        swapRouter.back()
    }

    fun rateClicked() {
        launchSwapRateDescription()
    }

    fun priceDifferenceClicked() {
        launchDescriptionBottomSheet(
            titleRes = R.string.swap_price_difference_title,
            descriptionRes = R.string.swap_price_difference_description
        )
    }

    fun slippageClicked() {
        launchDescriptionBottomSheet(
            titleRes = R.string.swap_slippage_title,
            descriptionRes = R.string.swap_slippage_description
        )
    }

    fun networkFeeClicked() {
        launchNetworkFeeDescription()
    }

    fun accountClicked() {
        launch {
            val chainWithAsset = chainWithAssetFlow.first()
            val addressModel = addressFlow.first()

            externalActions.showAddressActions(addressModel.address, chainWithAsset.chain)
        }
    }

    fun confirmButtonClicked() {
        launch {
            val validationSystem = swapInteractor.validationSystem()
            val payload = getValidationPayload() ?: return@launch

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                progressConsumer = _validationProgress.progressConsumer(),
                validationFailureTransformerCustom = ::formatValidationFailure,
                block = ::executeSwap
            )
        }
    }

    private fun createMaxActionProvider(): MaxActionProvider {
        return maxActionProviderFactory.create(
            assetInFlow = assetInFlow,
            assetOutFlow = assetOutFlow,
            field = Asset::transferableInPlanks,
            feeLoaderMixin = feeMixin,
            extractTotalFee = SwapFee::totalDeductedPlanks
        )
    }

    private fun executeSwap(validationPayload: SwapValidationPayload) = launch {
        swapInteractor.executeSwap(validationPayload.swapExecuteArgs, validationPayload.decimalFee)
            .onSuccess { navigateToNextScreen(validationPayload.swapExecuteArgs.assetIn) }
            .onFailure(::showError)

        _validationProgress.value = false
    }

    private fun navigateToNextScreen(asset: Chain.Asset) {
        swapRouter.openBalanceDetails(asset.fullId.toAssetPayload())
    }

    private suspend fun formatToSwapDetailsModel(confirmationState: SwapConfirmationState): SwapConfirmationDetailsModel {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val assetIn = confirmationState.swapQuote.assetIn
        val assetOut = confirmationState.swapQuote.assetOut
        val chainIn = chainRegistry.getChain(assetIn.chainId)
        val chainOut = chainRegistry.getChain(assetOut.chainId)
        return SwapConfirmationDetailsModel(
            assets = SwapAssetsView.Model(
                assetIn = formatAssetDetails(metaAccount, chainIn, assetIn, confirmationState.swapQuote.planksIn),
                assetOut = formatAssetDetails(metaAccount, chainOut, assetOut, confirmationState.swapQuote.planksOut)
            ),
            rate = formatRate(payload.rate, assetIn, assetOut),
            priceDifference = formatPriceDifference(confirmationState.swapQuote.priceImpact),
            slippage = payload.slippage.asPercent().format()
        )
    }

    private suspend fun formatAssetDetails(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        amountInPlanks: BigInteger
    ): SwapAssetView.Model {
        val amount = formatAmount(metaAccount, chainAsset, amountInPlanks)
        return SwapAssetView.Model(
            assetIcon = chainAsset.icon(),
            amount = amount,
            chainUi = mapChainToUi(chain),
        )
    }

    private fun formatRate(rate: BigDecimal, assetIn: Chain.Asset, assetOut: Chain.Asset): String {
        return swapRateFormatter.format(rate, assetIn, assetOut)
    }

    private fun formatPriceDifference(priceDifference: Percent): CharSequence? {
        return priceImpactFormatter.format(priceDifference)
    }

    private suspend fun formatAmount(metaAccount: MetaAccount, chainAsset: Chain.Asset, amount: BigInteger): AmountModel {
        val asset = walletRepository.getAsset(metaAccount.id, chainAsset)!!
        return mapAmountToAmountModel(amount, asset.token, includeZeroFiat = false, estimatedFiat = true)
    }

    private suspend fun getValidationPayload(): SwapValidationPayload? {
        val confirmationState = confirmationStateFlow.value ?: return null
        val swapFee = feeMixin.getDecimalFeeOrNull() ?: return null
        return swapInteractor.getValidationPayload(
            assetIn = confirmationState.swapQuote.assetIn,
            assetOut = confirmationState.swapQuote.assetOut,
            feeAsset = confirmationState.feeAsset,
            quoteArgs = confirmationState.swapQuoteArgs,
            swapQuote = confirmationState.swapQuote,
            swapFee = swapFee
        )
    }

    private fun formatValidationFailure(
        status: ValidationStatus.NotValid<SwapValidationFailure>,
        actions: ValidationFlowActions<SwapValidationPayload>
    ): TransformedFailure? {
        return viewModelScope.mapSwapValidationFailureToUI(
            resourceManager,
            status,
            actions,
            setNewFee = ::setNewFee,
            amountInSwapMaxAction = ::setMaxAmountIn,
            amountOutSwapMinAction = ::setMinAmountOut
        )
    }

    private fun setMaxAmountIn() {
        launch {
            maxActionFlow.value = MaxAction.ACTIVE
        }
    }

    private fun setMinAmountOut(asset: Chain.Asset, amount: Balance) {
        maxActionFlow.value = MaxAction.DISABLED
        val confirmationState = confirmationStateFlow.value ?: return
        runQuoting(
            confirmationState.swapQuoteArgs.copy(
                amount = amount,
                swapDirection = SwapDirection.SPECIFIED_OUT
            )
        )
    }

    private fun runQuoting(newSwapQuoteArgs: SwapQuoteArgs) {
        launch {
            val metaAccount = metaAccountFlow.first()
            val confirmationState = confirmationStateFlow.value ?: return@launch
            val swapQuote = swapInteractor.quote(newSwapQuoteArgs)
                .onFailure { }
                .getOrNull() ?: return@launch

            val nativeAsset = walletRepository.getAsset(metaAccount.id, newSwapQuoteArgs.tokenOut.configuration)!!
            val executeArgs = newSwapQuoteArgs.toExecuteArgs(swapQuote, confirmationState.feeAsset, nativeAsset)
            feeMixin.loadFeeV2Generic(
                coroutineScope = viewModelScope,
                feeConstructor = { swapInteractor.estimateFee(executeArgs) },
                onRetryCancelled = { }
            )

            confirmationStateFlow.value = confirmationState.copy(swapQuoteArgs = newSwapQuoteArgs, swapQuote = swapQuote)
        }
    }

    private fun setNewFee(fee: SwapFee) {
        launch {
            feeMixin.setFee(fee)
        }
    }

    private fun initConfirmationState() {
        launch {
            val swapQuote = swapConfirmationPayloadFormatter.mapSwapQuoteFromModel(payload.swapQuoteModel)
            val assetIn = swapQuote.assetIn
            val assetOut = swapQuote.assetOut
            val swapFee = swapConfirmationPayloadFormatter.mapFeeFromModel(payload.swapFee)
            val feeAsset = chainRegistry.asset(payload.feeAsset.fullChainAssetId)

            val quoteArgs = SwapQuoteArgs(
                tokenRepository.getToken(assetIn),
                tokenRepository.getToken(assetOut),
                swapQuote.editedBalance,
                swapQuote.direction,
                slippageFlow.first()
            )

            feeMixin.setFee(swapFee)
            confirmationStateFlow.value = SwapConfirmationState(
                swapQuoteArgs = quoteArgs,
                swapQuote = swapQuote,
                feeAsset = feeAsset
            )
        }
    }

    private fun handleMaxClick() {
        combineToPair(maxActionFlow, maxActionProvider.maxAvailableForAction)
            .filter { (maxAction, _) -> maxAction == MaxAction.ACTIVE }
            .mapNotNull { it.second?.balance }
            .distinctUntilChanged()
            .onEach {
                val confirmationState = confirmationStateFlow.value ?: return@onEach
                runQuoting(
                    confirmationState.swapQuoteArgs.copy(
                        amount = it,
                        swapDirection = SwapDirection.SPECIFIED_IN
                    )
                )
            }
            .launchIn(viewModelScope)
    }
}
