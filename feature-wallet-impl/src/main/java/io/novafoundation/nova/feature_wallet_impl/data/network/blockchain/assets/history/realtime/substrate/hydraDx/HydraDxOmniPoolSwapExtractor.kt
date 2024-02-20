package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate.hydraDx

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class HydraDxOmniPoolSwapExtractor(
    hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : BaseHydraDxSwapExtractor(hydraDxAssetIdConverter) {

    private val calls = listOf("buy", "sell")

    override fun isSwap(call: GenericCall.Instance): Boolean {
        return call.module.name == Modules.OMNIPOOL &&
            call.function.name in calls
    }

    override fun ExtrinsicVisit.extractSwapArgs(): SwapArgs {
        val swapExecutedEvent = events.findEvent(Modules.OMNIPOOL, "BuyExecuted")
            ?: events.findEvent(Modules.OMNIPOOL, "SellExecuted")

        return when {
            // successful swap, extract from event
            swapExecutedEvent != null -> {
                val (_, assetIn, assetOut, amountIn, amountOut) = swapExecutedEvent.arguments

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
                    amountIn = bindNumber(call.arguments["amount"]),
                    amountOut = bindNumber(call.arguments["min_buy_amount"])
                )
            }

            call.function.name == "buy" -> {
                SwapArgs(
                    assetIn = bindNumber(call.arguments["asset_in"]),
                    assetOut = bindNumber(call.arguments["asset_out"]),
                    amountIn = bindNumber(call.arguments["max_sell_amount"]),
                    amountOut = bindNumber(call.arguments["amount"])
                )
            }

            else -> error("Unknown call")
        }
    }
}
