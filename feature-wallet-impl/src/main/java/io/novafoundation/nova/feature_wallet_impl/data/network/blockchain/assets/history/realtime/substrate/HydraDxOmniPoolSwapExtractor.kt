package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findLastEvent
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.requireNativeFee
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall

class HydraDxOmniPoolSwapExtractor(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : SubstrateRealtimeOperationFetcher.Extractor {

    private val calls = listOf("buy", "sell")

    override suspend fun extractRealtimeHistoryUpdates(
        extrinsicVisit: ExtrinsicVisit,
        chain: Chain,
        chainAsset: Chain.Asset
    ): RealtimeHistoryUpdate.Type? {
        if (!extrinsicVisit.call.isSwap()) return null

        val (assetIdIn, assetIdOut, amountIn, amountOut) = extrinsicVisit.extractSwapArgs()

        val assetIn = hydraDxAssetIdConverter.toChainAssetOrNull(chain, assetIdIn) ?: return null
        val assetOut = hydraDxAssetIdConverter.toChainAssetOrNull(chain, assetIdOut) ?: return null

        val fee = extrinsicVisit.extractFee(chain)

        return RealtimeHistoryUpdate.Type.Swap(
            amountIn = ChainAssetWithAmount(assetIn, amountIn),
            amountOut = ChainAssetWithAmount(assetOut, amountOut),
            amountFee = fee,
            senderId = extrinsicVisit.origin,
            receiverId = extrinsicVisit.origin
        )
    }

    private fun ExtrinsicVisit.extractSwapArgs(): SwapArgs {
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
                    assetOut =  bindNumber(call.arguments["asset_out"]),
                    amountIn = bindNumber(call.arguments["amount"]),
                    amountOut = bindNumber(call.arguments["min_buy_amount"])
                )
            }

            call.function.name == "buy" -> {
                SwapArgs(
                    assetIn = bindNumber(call.arguments["asset_in"]),
                    assetOut =  bindNumber(call.arguments["asset_out"]),
                    amountIn = bindNumber(call.arguments["max_sell_amount"]),
                    amountOut = bindNumber(call.arguments["amount"])
                )
            }

            else -> error("Unknown call")
        }
    }

    private suspend fun ExtrinsicVisit.extractFee(chain: Chain): ChainAssetWithAmount {
        val feeDepositEvent = events.findLastEvent(Modules.CURRENCIES, "Deposited") ?: return nativeFee(chain)

        val (currencyIdRaw, _, amountRaw) = feeDepositEvent.arguments
        val currencyId = bindNumber(currencyIdRaw)

        val feeAsset = hydraDxAssetIdConverter.toChainAssetOrNull(chain, currencyId) ?: return nativeFee(chain)

        return ChainAssetWithAmount(feeAsset, bindNumber(amountRaw))
    }

    private fun ExtrinsicVisit.nativeFee(chain: Chain): ChainAssetWithAmount {
        return ChainAssetWithAmount(chain.utilityAsset, events.requireNativeFee())
    }

    private fun GenericCall.Instance.isSwap(): Boolean {
        return module.name == Modules.OMNIPOOL &&
            function.name in calls
    }

    private data class SwapArgs(val assetIn: HydraDxAssetId, val assetOut: HydraDxAssetId, val amountIn: HydraDxAssetId, val amountOut: HydraDxAssetId)
}
