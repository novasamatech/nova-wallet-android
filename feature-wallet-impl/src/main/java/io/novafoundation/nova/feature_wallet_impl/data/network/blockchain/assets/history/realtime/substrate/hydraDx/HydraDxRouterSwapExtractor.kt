package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate.hydraDx

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class HydraDxRouterSwapExtractor(
    hydraDxAssetIdConverter: io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter,
) : BaseHydraDxSwapExtractor(hydraDxAssetIdConverter) {

    private val calls = listOf("buy", "sell")

    override fun isSwap(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.ROUTER &&
            call.function.name in calls
    }

    override fun ExtrinsicVisit.extractSwapArgs(): SwapArgs {
        val swapExecutedEvent = events.findEvent(Modules.ROUTER, "RouteExecuted")
            ?: events.findEvent(Modules.ROUTER, "Executed")

        return when {
            // successful swap, extract from event
            swapExecutedEvent != null -> {
                val (assetIn, assetOut, amountIn, amountOut) = swapExecutedEvent.arguments

                SwapArgs(
                    assetIn = bindNumber(assetIn),
                    assetOut = bindNumber(assetOut),
                    amountIn = bindNumber(amountIn),
                    amountOut = bindNumber(amountOut)
                )
            }

            // failed swap, extract from call args
            call.function.name == "sell" -> {
                SwapArgs(
                    assetIn = bindNumber(call.arguments["asset_in"]),
                    assetOut = bindNumber(call.arguments["asset_out"]),
                    amountIn = bindNumber(call.arguments["amount_in"]),
                    amountOut = bindNumber(call.arguments["min_amount_out"])
                )
            }

            call.function.name == "buy" -> {
                SwapArgs(
                    assetIn = bindNumber(call.arguments["asset_in"]),
                    assetOut = bindNumber(call.arguments["asset_out"]),
                    amountIn = bindNumber(call.arguments["max_amount_in"]),
                    amountOut = bindNumber(call.arguments["amount_out"])
                )
            }

            else -> error("Unknown call")
        }
    }
}
