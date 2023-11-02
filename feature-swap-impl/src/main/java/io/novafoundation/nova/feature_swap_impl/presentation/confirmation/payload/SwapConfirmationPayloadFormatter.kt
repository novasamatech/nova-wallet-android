package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload

import io.novafoundation.nova.common.utils.asPercent
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset

class SwapConfirmationPayloadFormatter(
    private val chainRegistry: ChainRegistry
) {

    suspend fun mapSwapQuoteFromModel(model: SwapConfirmationPayload.SwapQuoteModel): SwapQuote {
        return SwapQuote(
            chainRegistry.asset(model.assetIn.fullChainAssetId),
            chainRegistry.asset(model.assetOut.fullChainAssetId),
            model.planksIn,
            model.planksOut,
            model.direction.mapFromModel(),
            model.priceImpact.asPercent()
        )
    }

    fun mapSwapQuoteToModel(model: SwapQuote): SwapConfirmationPayload.SwapQuoteModel {
        return SwapConfirmationPayload.SwapQuoteModel(
            model.assetIn.fullId.toAssetPayload(),
            model.assetOut.fullId.toAssetPayload(),
            model.planksIn,
            model.planksOut,
            model.direction.mapToModel(),
            model.priceImpact.value
        )
    }

    suspend fun mapFeeFromModel(model: SwapConfirmationPayload.FeeDetails): SwapFee {
        val minimumBalanceBuyIn = when (val minimumBalanceBuyIn = model.minimumBalanceBuyIn) {
            is SwapConfirmationPayload.FeeDetails.MinimumBalanceBuyIn.NeedsToBuyMinimumBalance -> {
                MinimumBalanceBuyIn.NeedsToBuyMinimumBalance(
                    chainRegistry.asset(minimumBalanceBuyIn.nativeAsset.fullChainAssetId),
                    minimumBalanceBuyIn.nativeMinimumBalance,
                    chainRegistry.asset(minimumBalanceBuyIn.commissionAsset.fullChainAssetId),
                    minimumBalanceBuyIn.commissionAssetToSpendOnBuyIn
                )
            }

            SwapConfirmationPayload.FeeDetails.MinimumBalanceBuyIn.NoBuyInNeeded -> MinimumBalanceBuyIn.NoBuyInNeeded
        }
        return SwapFee(InlineFee(model.amount), minimumBalanceBuyIn)
    }

    fun mapFeeToModel(swapFee: SwapFee): SwapConfirmationPayload.FeeDetails {
        val minimumBalanceBuyIn = when (val minimumBalanceBuyIn = swapFee.minimumBalanceBuyIn) {
            is MinimumBalanceBuyIn.NeedsToBuyMinimumBalance -> {
                val nativeAsset = minimumBalanceBuyIn.nativeAsset.fullId.toAssetPayload()
                val commissionAsset = minimumBalanceBuyIn.commissionAsset.fullId.toAssetPayload()
                SwapConfirmationPayload.FeeDetails.MinimumBalanceBuyIn.NeedsToBuyMinimumBalance(
                    nativeAsset,
                    minimumBalanceBuyIn.nativeMinimumBalance,
                    commissionAsset,
                    minimumBalanceBuyIn.commissionAssetToSpendOnBuyIn
                )
            }

            MinimumBalanceBuyIn.NoBuyInNeeded -> SwapConfirmationPayload.FeeDetails.MinimumBalanceBuyIn.NoBuyInNeeded
        }
        return SwapConfirmationPayload.FeeDetails(swapFee.networkFee.amount, minimumBalanceBuyIn)
    }

    fun SwapConfirmationPayload.Direction.mapFromModel(): SwapDirection {
        return when (this) {
            SwapConfirmationPayload.Direction.SPECIFIED_IN -> SwapDirection.SPECIFIED_IN
            SwapConfirmationPayload.Direction.SPECIFIED_OUT -> SwapDirection.SPECIFIED_OUT
        }
    }

    fun SwapDirection.mapToModel(): SwapConfirmationPayload.Direction {
        return when (this) {
            SwapDirection.SPECIFIED_IN -> SwapConfirmationPayload.Direction.SPECIFIED_IN
            SwapDirection.SPECIFIED_OUT -> SwapConfirmationPayload.Direction.SPECIFIED_OUT
        }
    }
}
