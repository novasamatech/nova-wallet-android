package io.novafoundation.nova.feature_swap_impl.presentation.fee

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData.SwapFeeType
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteModel
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.stateFlow
import io.novafoundation.nova.feature_swap_impl.presentation.fee.model.SwapSegmentFeeModel
import io.novafoundation.nova.feature_swap_impl.presentation.fee.model.SwapSegmentFeeModel.FeeOperationModel
import io.novafoundation.nova.feature_swap_impl.presentation.fee.model.SwapSegmentFeeModel.SwapComponentFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.toFeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.map

class SwapFeeViewModel(
    private val swapInteractor: SwapInteractor,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    swapStateStoreProvider: SwapStateStoreProvider
) : BaseViewModel() {

    private val swapStateFlow = swapStateStoreProvider.stateFlow(viewModelScope)

    val swapFeeSegments = swapStateFlow
        .map { it.fee.toSwapFeeSegments() }
        .withSafeLoading()
        .shareInBackground()

    val totalFee = swapStateFlow.map {
        val fee = swapInteractor.calculateTotalFiatPrice(it.fee)
        fee.formatAsCurrency()
    }.shareInBackground()

    private suspend fun SwapFee.toSwapFeeSegments(): List<SwapSegmentFeeModel> {
        val allTokens = swapInteractor.getAllFeeTokens(this)

        return segments.map { segment ->
            val operationData = segment.operation.constructDisplayData()
            val feeDisplayData = segment.fee.constructDisplayData()

            SwapSegmentFeeModel(
                operation = operationData.toFeeOperationModel(),
                feeComponents = feeDisplayData.toFeeComponentModels(allTokens)
            )
        }
    }

    private fun AtomicOperationFeeDisplayData.toFeeComponentModels(
        tokens: Map<FullChainAssetId, Token>
    ): List<SwapComponentFeeModel> {
        return components.map { feeDisplaySegment ->
            SwapComponentFeeModel(
                label = feeDisplaySegment.type.formatLabel(),
                individualFees = feeDisplaySegment.fees.map { individualFee ->
                    val token = tokens.getValue(individualFee.asset.fullId)
                    mapAmountToAmountModel(individualFee.amount, token).toFeeDisplay()
                }
            )
        }
    }

    private fun SwapFeeType.formatLabel(): String {
        return when (this) {
            SwapFeeType.NETWORK -> resourceManager.getString(R.string.network_fee)
            SwapFeeType.CROSS_CHAIN -> resourceManager.getString(R.string.wallet_send_cross_chain_fee)
        }
    }

    private suspend fun AtomicOperationDisplayData.toFeeOperationModel(): FeeOperationModel {
        return when (this) {
            is AtomicOperationDisplayData.Swap -> toFeeOperationModel()
            is AtomicOperationDisplayData.Transfer -> toFeeOperationModel()
        }
    }

    private suspend fun AtomicOperationDisplayData.Swap.toFeeOperationModel(): FeeOperationModel {
        val chain = chainRegistry.getChain(from.chainAssetId.chainId)
        val chains = listOf(mapChainToUi(chain))

        return FeeOperationModel(
            label = resourceManager.getString(R.string.swap_route_segment_swap_title),
            swapRoute = SwapRouteModel(chains)
        )
    }

    private suspend fun AtomicOperationDisplayData.Transfer.toFeeOperationModel(): FeeOperationModel {
        val chainFrom = chainRegistry.getChain(from.chainId)
        val chainTo = chainRegistry.getChain(to.chainId)

        val chains = listOf(mapChainToUi(chainFrom), mapChainToUi(chainTo))

        return FeeOperationModel(
            label = resourceManager.getString(R.string.swap_route_segment_transfer_title),
            swapRoute = SwapRouteModel(chains)
        )
    }
}
