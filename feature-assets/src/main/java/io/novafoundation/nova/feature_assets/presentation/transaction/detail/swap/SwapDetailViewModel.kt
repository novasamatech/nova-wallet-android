package io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.description.launchNetworkFeeDescription
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.ChainAssetWithAmountParcelModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_swap_api.domain.model.rateAgainst
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapDirectionParcel
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.view.SwapAssetView
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SwapDetailViewModel(
    private val router: AssetsRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry,
    private val externalActions: ExternalActions.Presentation,
    private val arbitraryTokenUseCase: ArbitraryTokenUseCase,
    private val walletUiUseCase: WalletUiUseCase,
    private val swapRateFormatter: SwapRateFormatter,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val assetIconProvider: AssetIconProvider,
    val operation: OperationParcelizeModel.Swap,
    private val amountFormatter: AmountFormatter
) : BaseViewModel(),
    ExternalActions by externalActions,
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher {

    private val tokenIn = historicalTokenFlow(operation.amountIn)
    private val tokenOut = historicalTokenFlow(operation.amountOut)
    private val tokenFee = historicalTokenFlow(operation.amountFee)

    private val originChain by lazyAsync {
        chainRegistry.getChain(operation.amountIn.assetId.chainId)
    }

    val originAddressModelFlow = flowOf {
        getIcon(operation.originAddress)
    }
        .shareInBackground()

    val walletUi = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeModel = tokenFee.map {
        amountFormatter.formatAmountToAmountModel(operation.amountFee.amount, it)
    }
        .withSafeLoading()
        .shareInBackground()

    val assetInModel = tokenIn.map { tokenIn ->
        createAssetSwapModel(tokenIn, operation.amountIn.amount, income = false)
    }.shareInBackground()

    val assetOutModel = tokenOut.map { tokenOut ->
        createAssetSwapModel(tokenOut, operation.amountOut.amount, income = true)
    }.shareInBackground()

    val amountModel = amountModelFlow()
        .shareInBackground()

    val rate = combine(tokenIn, tokenOut) { tokenIn, tokenOut ->
        val assetIn = tokenIn.configuration.withAmount(operation.amountIn.amount)
        val assetOut = tokenOut.configuration.withAmount(operation.amountOut.amount)

        val rate = assetIn rateAgainst assetOut

        swapRateFormatter.format(rate, assetIn.chainAsset, assetOut.chainAsset)
    }.shareInBackground()

    fun backClicked() {
        router.back()
    }

    private suspend fun getIcon(address: String): AddressModel {
        return addressIconGenerator.createAccountAddressModel(originChain(), address)
    }

    fun transactionHashClicked() = operation.transactionHash?.let {
        showExternalActions(ExternalActions.Type.Extrinsic(it))
    }

    fun originAddressClicked() = launch {
        externalActions.showAddressActions(operation.originAddress, originChain())
    }

    fun rateClicked() {
        descriptionBottomSheetLauncher.launchSwapRateDescription()
    }

    fun feeClicked() {
        descriptionBottomSheetLauncher.launchNetworkFeeDescription()
    }

    fun repeatOperationClicked() {
        val amount = if (operation.amountIsAssetIn) operation.amountIn.amount else operation.amountOut.amount
        val direction = if (operation.amountIsAssetIn) SwapDirectionParcel.SPECIFIED_IN else SwapDirectionParcel.SPECIFIED_OUT
        val payload = SwapSettingsPayload.RepeatOperation(
            assetIn = operation.amountIn.assetId,
            assetOut = operation.amountOut.assetId,
            amount = amount,
            direction = direction
        )
        router.openSwapSetupAmount(payload)
    }

    private fun showExternalActions(type: ExternalActions.Type) = launch {
        externalActions.showExternalActions(type, originChain())
    }

    private fun historicalTokenFlow(parcel: ChainAssetWithAmountParcelModel): Flow<TokenBase> {
        return flowOf {
            val chainAsset = chainRegistry.asset(parcel.assetId.fullChainAssetId)

            arbitraryTokenUseCase.historicalToken(chainAsset, operation.timeMillis.milliseconds)
        }
            .shareInBackground()
    }

    private suspend fun createAssetSwapModel(
        token: TokenBase,
        amount: Balance,
        income: Boolean
    ): SwapAssetView.Model {
        return SwapAssetView.Model(
            assetIcon = assetIconProvider.getAssetIconOrFallback(token.configuration),
            amount = amountFormatter.formatAmountToAmountModel(amount, token, AmountConfig(estimatedFiat = true)),
            chainUi = mapChainToUi(chainRegistry.getChain(token.configuration.chainId)),
            amountTextColorRes = if (income) R.color.text_positive else R.color.text_primary
        )
    }

    private fun amountModelFlow(): Flow<AmountModel> {
        return if (operation.amountIsAssetIn) {
            tokenIn.map {
                amountFormatter.formatAmountToAmountModel(
                    operation.amountIn.amount,
                    it,
                    AmountConfig(estimatedFiat = true, tokenAmountSign = AmountSign.NEGATIVE)
                )
            }
        } else {
            tokenOut.map {
                amountFormatter.formatAmountToAmountModel(
                    operation.amountOut.amount,
                    it,
                    AmountConfig(estimatedFiat = true, tokenAmountSign = AmountSign.POSITIVE)
                )
            }
        }
    }
}
