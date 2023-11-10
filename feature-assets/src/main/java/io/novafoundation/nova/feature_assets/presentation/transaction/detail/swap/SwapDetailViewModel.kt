package io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
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
import io.novafoundation.nova.feature_account_api.presenatation.chain.icon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.ChainAssetWithAmountParcelModel
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_swap_api.domain.model.rateAgainst
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.view.SwapAssetView
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
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
    val operation: OperationParcelizeModel.Swap,
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
        mapAmountToAmountModel(operation.amountFee.amount, it)
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

    fun originAddressClicked() {
        showExternalActions(ExternalActions.Type.Address(operation.originAddress))
    }

    fun rateClicked() {
        descriptionBottomSheetLauncher.launchSwapRateDescription()
    }

    fun feeClicked() {
        descriptionBottomSheetLauncher.launchNetworkFeeDescription()
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
            assetIcon = token.configuration.icon(),
            amount = mapAmountToAmountModel(amount, token, estimatedFiat = true),
            chainUi = mapChainToUi(chainRegistry.getChain(token.configuration.chainId)),
            amountTextColorRes = if (income) R.color.text_positive else R.color.text_primary
        )
    }

    private fun amountModelFlow(): Flow<AmountModel> {
        return if (operation.amountIsAssetIn) {
            tokenIn.map {
                mapAmountToAmountModel(operation.amountIn.amount, it, estimatedFiat = true, tokenAmountSign = AmountSign.NEGATIVE)
            }
        } else {
            tokenOut.map {
                mapAmountToAmountModel(operation.amountOut.amount, it, estimatedFiat = true, tokenAmountSign = AmountSign.POSITIVE)
            }
        }
    }
}
