package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_TRANSPARENT
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.asPercent
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.icon
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.editedBalance
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.SwapRateFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.model.SwapConfirmationDetailsModel
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayloadFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.main.mapSwapValidationFailureToUI
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class SwapConfirmationState(
    val swapQuoteArgs: SwapQuoteArgs,
    val swapQuote: SwapQuote,
    val feeAsset: Chain.Asset,
    val swapFee: SwapFee
)

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
    private val swapConfirmationPayloadFormatter: SwapConfirmationPayloadFormatter
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val confirmationStateFlow = MutableStateFlow<SwapConfirmationState?>(null)

    private val metaAccount = accountRepository.selectedMetaAccountFlow()
        .shareInBackground()

    private val slippageConfigFlow = confirmationStateFlow
        .filterNotNull()
        .map { swapInteractor.slippageConfig(it.swapQuote.assetIn.chainId) }
        .filterNotNull()
        .shareInBackground()

    private val slippageFlow = flowOf { payload.slippage.asPercent() }
        .shareInBackground()

    private val slippageAlertMixin = slippageAlertMixinFactory.create(slippageConfigFlow, slippageFlow)

    private val chainInFlow = confirmationStateFlow
        .filterNotNull()
        .map { chainRegistry.getChain(it.swapQuote.assetIn.chainId) }
        .shareInBackground()

    private val _validationProgress = MutableStateFlow(false)

    val validationProgress = _validationProgress.map { submitting ->
        if (submitting) {
            ButtonState.PROGRESS
        } else {
            ButtonState.NORMAL
        }
    }

    val swapDetails = confirmationStateFlow
        .filterNotNull()
        .map { formatToSwapDetailsModel(it) }

    val wallet: Flow<WalletModel> = metaAccount.map { walletUiUseCase.walletUiFor(it) }

    val addressFlow: Flow<AddressModel> = combine(chainInFlow, metaAccount) { chain, metaAccount ->
        val accountId = metaAccount.requireAddressIn(chain)
        addressIconGenerator.createAddressModel(chain, accountId, 5, accountName = null, background = BACKGROUND_TRANSPARENT)
    }

    val slippageAlertMessage: Flow<String?> = slippageAlertMixin.slippageAlertMessage

    init {
        launch {
            initConfirmationState()
        }
    }

    fun rateClicked() {
        TODO("Not yet implemented")
    }

    fun priceDifferenceClicked() {
        TODO("Not yet implemented")
    }

    fun slippageClicked() {
        TODO("Not yet implemented")
    }

    fun networkFeeClicked() {
        TODO("Not yet implemented")
    }

    fun accountClicked() {
        launch {
            val chainFlow = chainInFlow.first()
            val addressModel = addressFlow.first()

            val type = ExternalActions.Type.Address(addressModel.address)

            externalActions.showExternalActions(type, chainFlow)
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
            ) { validPayload ->
                executeSwap(validPayload.swapExecuteArgs)
            }
        }
    }

    private fun executeSwap(swapExecuteArgs: SwapExecuteArgs) {
        launch {
            val result = withContext(Dispatchers.Default) { swapInteractor.executeSwap(swapExecuteArgs) }
            result.onSuccess {
                swapRouter.finishSwapFlow(swapExecuteArgs.assetIn.fullId.toAssetPayload())
            }.onFailure {
                Log.e("SwapError", "Failed to execute swap", it)
            }
        }
    }

    private suspend fun formatToSwapDetailsModel(confirmationState: SwapConfirmationState): SwapConfirmationDetailsModel {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val assetIn = confirmationState.swapQuote.assetIn
        val assetOut = confirmationState.swapQuote.assetOut
        val chainIn = chainRegistry.getChain(assetIn.chainId)
        val chainOut = chainRegistry.getChain(assetOut.chainId)
        return SwapConfirmationDetailsModel(
            assetInDetails = formatAssetDetails(metaAccount, chainIn, assetIn, confirmationState.swapQuote.planksIn),
            assetOutDetails = formatAssetDetails(metaAccount, chainOut, assetOut, confirmationState.swapQuote.planksOut),
            rate = formatRate(payload.rate, assetIn, assetOut),
            priceDifference = formatPriceDifference(confirmationState.swapQuote.priceImpact),
            slippage = payload.slippage.asPercent().format(),
            networkFee = formatAmount(metaAccount, assetIn, confirmationState.swapFee.networkFee.amount)
        )
    }

    private suspend fun getChainWithAsset(assetPayload: AssetPayload): ChainWithAsset {
        val fullChainAssetId = assetPayload.fullChainAssetId
        return chainRegistry.chainWithAsset(fullChainAssetId.chainId, fullChainAssetId.assetId)
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
            networkImage = Icon.FromLink(chain.icon),
            networkName = chain.name
        )
    }

    private fun formatRate(rate: BigDecimal, assetIn: Chain.Asset, assetOut: Chain.Asset): String {
        return swapRateFormatter.format(rate, assetIn, assetOut)
    }

    private fun formatPriceDifference(priceDifference: Percent): CharSequence? {
        return priceImpactFormatter.format(priceDifference)
    }

    private suspend fun formatNetworkFee(metaAccount: MetaAccount, assetIn: Chain.Asset, swapFee: SwapFee): AmountModel {
        return formatAmount(metaAccount, assetIn, swapFee.networkFee.amount)
    }

    private suspend fun formatAmount(metaAccount: MetaAccount, chainAsset: Chain.Asset, amount: BigInteger): AmountModel {
        val asset = walletRepository.getAsset(metaAccount.id, chainAsset)!!
        return mapAmountToAmountModel(amount, asset.token, includeZeroFiat = false, estimatedFiat = true)
    }

    private suspend fun getValidationPayload(): SwapValidationPayload? {
        val confirmationState = confirmationStateFlow.value ?: return null
        return swapInteractor.getValidationPayload(
            assetIn = confirmationState.swapQuote.assetIn,
            assetOut = confirmationState.swapQuote.assetOut,
            feeAsset = confirmationState.feeAsset,
            quoteArgs = confirmationState.swapQuoteArgs,
            swapQuote = confirmationState.swapQuote,
            swapFee = confirmationState.swapFee
        )
    }

    private fun formatValidationFailure(
        status: ValidationStatus.NotValid<SwapValidationFailure>,
        actions: ValidationFlowActions<SwapValidationPayload>
    ): TransformedFailure? {
        return null

        return viewModelScope.mapSwapValidationFailureToUI(
            resourceManager,
            status,
            actions,
            TODO(),
            TODO(),
            TODO()
        )
    }

    private suspend fun initConfirmationState() {
        val swapQuote = swapConfirmationPayloadFormatter.mapSwapQuoteFromModel(payload.swapQuoteModel)
        val swapFee = swapConfirmationPayloadFormatter.mapFeeFromModel(payload.swapFee)
        val assetIn = swapQuote.assetIn
        val assetOut = swapQuote.assetOut
        val feeAsset = chainRegistry.asset(payload.feeAsset.fullChainAssetId)
        val quoteArgs = SwapQuoteArgs(
            tokenRepository.getToken(assetIn),
            tokenRepository.getToken(assetOut),
            swapQuote.editedBalance,
            swapQuote.direction,
            slippageFlow.first()
        )

        confirmationStateFlow.value = SwapConfirmationState(
            swapQuoteArgs = quoteArgs,
            swapQuote = swapQuote,
            feeAsset = feeAsset,
            swapFee = swapFee
        )
    }
}
