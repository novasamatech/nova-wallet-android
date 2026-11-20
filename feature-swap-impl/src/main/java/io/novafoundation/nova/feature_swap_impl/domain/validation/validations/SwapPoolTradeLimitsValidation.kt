package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.swapDirection
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.TradeAmountLimitedEdge
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import kotlinx.coroutines.CancellationException

/**
 * Static preflight against the quoted gross amounts of every pool hop.
 * RealSwapService.swap preserves the first atomic operation's direction and executes all later operations
 * as sells. Fee segments define these operation boundaries; the quote supplies each hop's input/output estimate.
 *
 * Execution can change amounts through additionalAmountForSwap and subsequent actualReceivedAmount corrections.
 * Passing this check does not prove that those future corrected amounts satisfy the limits. In particular,
 * nonlinear AMM hop amounts must not be approximated by scaling every quote with an operation-wide ratio.
 */
class SwapPoolTradeLimitsValidation(
    private val assetsValidationContext: AssetsValidationContext
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        return runCatching { validateTradeLimits(value) }.getOrElse { cause ->
            if (cause is CancellationException) throw cause

            SwapValidationFailure.PoolTradeLimitsUnavailable(cause).validationError()
        }
    }

    private suspend fun validateTradeLimits(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val quotedPath = value.swapQuote.quotedPath
        val path = quotedPath.path
        val directions = operationDirections(path, value.fee.segments)

        path.forEachIndexed { index, quotedEdge ->
            val direction = directions[index]
            val isUserInputHop = direction == quotedPath.direction && when (direction) {
                SwapDirection.SPECIFIED_IN -> index == 0
                SwapDirection.SPECIFIED_OUT -> index == path.lastIndex
            }
            val status = validateHop(quotedEdge, quotedPath.direction, direction, isUserInputHop)
            if (status is ValidationStatus.NotValid) return status
        }

        return valid()
    }

    private fun operationDirections(
        path: List<QuotedEdge<SwapGraphEdge>>,
        segments: List<SwapFee.SwapSegment>
    ): List<SwapDirection> {
        require(path.isNotEmpty() && segments.isNotEmpty()) { "Missing swap execution operations" }
        require(path.zipWithNext().all { (first, next) -> first.edge.to == next.edge.from }) { "Disconnected quoted route" }
        val directions = mutableListOf<SwapDirection>()
        var nextHop = 0

        segments.forEachIndexed { operationIndex, segment ->
            val operation = segment.operation
            require(nextHop < path.size && path[nextHop].edge.from == operation.assetIn) { "Operation does not match quoted route" }
            // Quoted graph paths do not revisit assets; an ambiguous or missing boundary is invalid operation data.
            val lastHop = (nextHop..path.lastIndex).single { path[it].edge.to == operation.assetOut }
            val direction = if (operationIndex == 0) operation.estimatedSwapLimit.swapDirection else SwapDirection.SPECIFIED_IN
            repeat(lastHop - nextHop + 1) { directions.add(direction) }
            nextHop = lastHop + 1
        }
        require(nextHop == path.size) { "Execution operations do not cover the quoted route" }

        return directions
    }

    private suspend fun validateHop(
        quotedEdge: QuotedEdge<SwapGraphEdge>,
        quoteDirection: SwapDirection,
        executionDirection: SwapDirection,
        isUserInputHop: Boolean
    ): ValidationStatus<SwapValidationFailure> {
        val limitedEdge = quotedEdge.edge as? TradeAmountLimitedEdge ?: return valid()
        // quotedAmount is the input for a sell quote and the output for a buy quote; quote is its counterpart.
        val amount = if (executionDirection == quoteDirection) quotedEdge.quotedAmount else quotedEdge.quote
        val minimum = when (executionDirection) {
            SwapDirection.SPECIFIED_IN -> limitedEdge.minAllowedAmountIn()
            SwapDirection.SPECIFIED_OUT -> limitedEdge.minAllowedAmountOut()
        }
        val maximum = when (executionDirection) {
            SwapDirection.SPECIFIED_IN -> limitedEdge.maxAllowedAmountIn()
            SwapDirection.SPECIFIED_OUT -> limitedEdge.maxAllowedAmountOut()
        }
        val belowMinimum = minimum != null && amount < minimum
        val aboveMaximum = maximum != null && amount > maximum
        if (!belowMinimum && !aboveMaximum) return valid()

        val limitedAssetId = if (executionDirection == SwapDirection.SPECIFIED_IN) quotedEdge.edge.from else quotedEdge.edge.to
        val limitedAsset = assetsValidationContext.getAsset(limitedAssetId).token.configuration
        val failure: SwapValidationFailure = if (belowMinimum) {
            SwapValidationFailure.AmountBelowPoolTradeMinimum(limitedAsset, requireNotNull(minimum))
        } else {
            SwapValidationFailure.AmountExceedsPoolTradeLimit(
                limitedAsset = limitedAsset,
                maxAmount = requireNotNull(maximum),
                direction = executionDirection,
                isUserInputAdjustable = isUserInputHop
            )
        }
        return failure.validationError()
    }
}
