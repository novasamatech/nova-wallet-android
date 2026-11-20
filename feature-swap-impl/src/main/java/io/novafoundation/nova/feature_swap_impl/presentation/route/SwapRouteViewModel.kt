package io.novafoundation.nova.feature_swap_impl.presentation.route

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_api.domain.model.allBasicFees
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapOperationLoader
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.feature_swap_impl.presentation.route.model.SwapRouteItemModel
import io.novafoundation.nova.feature_swap_impl.presentation.route.view.TokenAmountModel
import io.novafoundation.nova.feature_wallet_api.domain.model.FiatAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map

class SwapRouteViewModel(
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    private val swapInteractor: SwapInteractor,
    private val operationLoader: SwapOperationLoader,
    private val chainRegistry: ChainRegistry,
    private val assetIconProvider: AssetIconProvider,
    private val router: SwapRouter,
    private val resourceManager: ResourceManager
) : BaseViewModel(), Retriable by operationLoader {

    val swapRoute = operationLoader.state(viewModelScope)
        .map { it.fee.toSwapRouteUi() }
        .withSafeLoading()
        .onError { Log.e("Swaps", "Could not prepare the swap route", it) }
        .shareInBackground()

    private suspend fun SwapFee.toSwapRouteUi(): List<SwapRouteItemModel> {
        val fiatFees = runCatching { swapInteractor.calculateSegmentFiatPrices(this) }.getOrElse { error ->
            if (error is CancellationException) throw error
            Log.w("Swaps", "Could not price route fees, showing their token amounts", error)
            emptyList()
        }

        return segments.mapIndexed { index, segment ->
            val displayData = segment.operation.constructDisplayData(segment.netFlow)
            val fee = formatStepFee(segment, fiatFees.getOrNull(index))
            when (displayData) {
                is AtomicOperationDisplayData.Swap -> displayData.toUi(fee, index)
                is AtomicOperationDisplayData.Transfer -> displayData.toUi(fee, index)
            }
        }
    }

    private fun formatStepFee(segment: SwapFee.SwapSegment, fiat: FiatAmount?): String {
        val amount = fiat?.let { fiatFormatter.formatFiat(it.price, it.currency) } ?: formatTokenFees(segment)
        return resourceManager.getString(R.string.common_fee_with_label, amount)
    }

    private fun formatTokenFees(segment: SwapFee.SwapSegment): String {
        return segment.fee.allBasicFees()
            .groupBy { it.asset.fullId }
            .values.joinToString(" + ") { fees ->
                tokenFormatter.formatToken(fees.sumOf { it.amount }, fees.first().asset)
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
