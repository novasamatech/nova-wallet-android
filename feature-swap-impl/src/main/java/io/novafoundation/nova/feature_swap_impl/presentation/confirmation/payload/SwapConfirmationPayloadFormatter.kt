package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload

import io.novafoundation.nova.common.utils.asPercent
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_api.domain.model.QuotePath
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.presentation.model.mapFromModel
import io.novafoundation.nova.feature_swap_api.presentation.model.mapToModel
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset

class SwapConfirmationPayloadFormatter(
    private val chainRegistry: ChainRegistry
) {

    suspend fun mapSwapQuoteFromModel(model: SwapConfirmationPayload.SwapQuoteModel): SwapQuote {
        return with(model) {
            SwapQuote(
                amountIn = chainRegistry.asset(assetIn.fullChainAssetId).withAmount(planksIn),
                amountOut = chainRegistry.asset(assetOut.fullChainAssetId).withAmount(planksOut),
                direction = model.direction.mapFromModel(),
                priceImpact = model.priceImpact.asPercent(),
                path = QuotePath(
                    segments = model.path.map {
                        QuotePath.Segment(
                            from = it.from.fullChainAssetId,
                            to = it.to.fullChainAssetId,
                            sourceId = it.sourceId,
                            sourceParams = it.sourceParams
                        )
                    }
                )
            )
        }
    }

    fun mapSwapQuoteToModel(model: SwapQuote): SwapConfirmationPayload.SwapQuoteModel {
        return SwapConfirmationPayload.SwapQuoteModel(
            assetIn = model.assetIn.fullId.toAssetPayload(),
            assetOut = model.assetOut.fullId.toAssetPayload(),
            planksIn = model.planksIn,
            planksOut = model.planksOut,
            direction = model.direction.mapToModel(),
            priceImpact = model.priceImpact.value,
            path = model.path.segments.map {
                SwapConfirmationPayload.SwapQuotePathModel(
                    from = it.from.toAssetPayload(),
                    to = it.to.toAssetPayload(),
                    sourceId = it.sourceId,
                    sourceParams = it.sourceParams
                )
            }
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

        val decimalFee = mapFeeFromParcel(model.networkFee)

        return SwapFee(decimalFee.networkFee, minimumBalanceBuyIn)
    }

    fun mapFeeToModel(swapFee: GenericDecimalFee<SwapFee>): SwapConfirmationPayload.FeeDetails {
        val minimumBalanceBuyIn = when (val minimumBalanceBuyIn = swapFee.genericFee.minimumBalanceBuyIn) {
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
        return SwapConfirmationPayload.FeeDetails(mapFeeToParcel(swapFee), minimumBalanceBuyIn)
    }
}
