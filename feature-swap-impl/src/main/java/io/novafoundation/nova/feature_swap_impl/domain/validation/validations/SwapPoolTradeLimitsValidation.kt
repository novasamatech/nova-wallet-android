package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.TradeAmountLimitedEdge
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext

/**
 * Checks that no hop of the quoted route exceeds the trade size limit of its pool ([TradeAmountLimitedEdge]),
 * e.g. XYK.MaxInRatio / XYK.MaxOutRatio on Hydration. Without this check such a trade quotes fine
 * but reverts on-chain, so the user pays the network fee for a failed swap
 *
 * [QuotedEdge.quotedAmount] is the "given" amount of the hop quote: the hop's input for
 * [SwapDirection.SPECIFIED_IN] and the hop's output for [SwapDirection.SPECIFIED_OUT].
 * This is exactly the value the per-direction limits apply to, so no additional
 * direction-based swapping of [QuotedEdge.quotedAmount]/[QuotedEdge.quote] is needed here
 */
class SwapPoolTradeLimitsValidation(
    private val assetsValidationContext: AssetsValidationContext
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val quotedPath = value.swapQuote.quotedPath
        val direction = quotedPath.direction
        val path = quotedPath.path

        path.forEachIndexed { index, quotedEdge ->
            val limitedEdge = quotedEdge.edge as? TradeAmountLimitedEdge ?: return@forEachIndexed

            val maxAllowedAmount = when (direction) {
                SwapDirection.SPECIFIED_IN -> limitedEdge.maxAllowedAmountIn()
                SwapDirection.SPECIFIED_OUT -> limitedEdge.maxAllowedAmountOut()
            } ?: return@forEachIndexed

            if (quotedEdge.quotedAmount > maxAllowedAmount) {
                val limitedAssetId = when (direction) {
                    SwapDirection.SPECIFIED_IN -> quotedEdge.edge.from
                    SwapDirection.SPECIFIED_OUT -> quotedEdge.edge.to
                }
                val limitedAsset = assetsValidationContext.getAsset(limitedAssetId).token.configuration

                val isUserInputHop = when (direction) {
                    SwapDirection.SPECIFIED_IN -> index == 0
                    SwapDirection.SPECIFIED_OUT -> index == path.lastIndex
                }

                return SwapValidationFailure.AmountExceedsPoolTradeLimit(
                    limitedAsset = limitedAsset,
                    maxAmount = maxAllowedAmount,
                    direction = direction,
                    isUserInputAdjustable = isUserInputHop
                ).validationError()
            }
        }

        return valid()
    }
}
