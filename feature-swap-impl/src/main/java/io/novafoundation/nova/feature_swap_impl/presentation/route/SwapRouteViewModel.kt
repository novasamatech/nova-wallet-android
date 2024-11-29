package io.novafoundation.nova.feature_swap_impl.presentation.route

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.stateFlow
import io.novafoundation.nova.feature_swap_impl.presentation.route.model.SwapRouteItemModel
import io.novafoundation.nova.feature_swap_impl.presentation.route.view.TokenAmountModel
import io.novafoundation.nova.feature_wallet_api.domain.model.FiatAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.map

class SwapRouteViewModel(
    private val swapInteractor: SwapInteractor,
    private val swapStateStoreProvider: SwapStateStoreProvider,
    private val chainRegistry: ChainRegistry,
    private val assetIconProvider: AssetIconProvider,
    private val router: SwapRouter,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val swapRoute = swapStateStoreProvider.stateFlow(viewModelScope)
        .map { it.fee.toSwapRouteUi() }
        .withSafeLoading()
        .shareInBackground()

    private suspend fun SwapFee.toSwapRouteUi(): List<SwapRouteItemModel> {
        val pricedFees = swapInteractor.calculateSegmentFiatPrices(this)

        return pricedFees.zip(segments).mapIndexed { index, (pricedFee, segment) ->
            val displayData = segment.operation.constructDisplayData()
            displayData.toUi(pricedFee, id = index)
        }
    }

    private suspend fun AtomicOperationDisplayData.toUi(
        fee: FiatAmount,
        id: Int
    ): SwapRouteItemModel {
        val formattedFee = fee.formatAsCurrency()
        val feeWithLabel = resourceManager.getString(R.string.common_fee_with_label, formattedFee)

        return when (this) {
            is AtomicOperationDisplayData.Swap -> toUi(feeWithLabel, id)
            is AtomicOperationDisplayData.Transfer -> toUi(feeWithLabel, id)
        }
    }

    private suspend fun AtomicOperationDisplayData.Transfer.toUi(fee: String, id: Int): SwapRouteItemModel.Transfer {
        val (chainFrom, assetFrom) = chainRegistry.chainWithAsset(from)
        val assetFromIcon = assetIconProvider.getAssetIconOrFallback(assetFrom)

        val chainTo = chainRegistry.getChain(to.chainId)

        return SwapRouteItemModel.Transfer(
            id = id,
            amount = TokenAmountModel.from(assetFrom, assetFromIcon, amount),
            fee = fee,
            originChainName = chainFrom.name,
            destinationChainName = chainTo.name
        )
    }

    private suspend fun AtomicOperationDisplayData.Swap.toUi(fee: String, id: Int): SwapRouteItemModel.Swap {
        val (chainFrom, assetFrom) = chainRegistry.chainWithAsset(from.chainAssetId)
        val assetFromIcon = assetIconProvider.getAssetIconOrFallback(assetFrom)

        val assetTo = chainRegistry.asset(to.chainAssetId)
        val assetToIcon = assetIconProvider.getAssetIconOrFallback(assetTo)

        return SwapRouteItemModel.Swap(
            id = id,
            amountFrom = TokenAmountModel.from(assetFrom, assetFromIcon, from.amount),
            amountTo = TokenAmountModel.from(assetTo, assetToIcon, to.amount),
            fee = fee,
            chain = chainFrom.name
        )
    }

    fun backClicked() {
        router.back()
    }
}
