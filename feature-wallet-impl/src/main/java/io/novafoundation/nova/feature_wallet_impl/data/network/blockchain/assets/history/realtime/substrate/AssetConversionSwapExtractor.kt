package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.feature_xcm_api.multiLocation.bindMultiLocation
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.assetTxFeePaidEvent
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findAllOfType
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.requireNativeFee
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

class AssetConversionSwapExtractor(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
) : SubstrateRealtimeOperationFetcher.Extractor {

    private val calls = listOf("swap_exact_tokens_for_tokens", "swap_tokens_for_exact_tokens")

    override suspend fun extractRealtimeHistoryUpdates(
        extrinsicVisit: ExtrinsicVisit,
        chain: Chain,
        chainAsset: Chain.Asset
    ): RealtimeHistoryUpdate.Type? {
        val call = extrinsicVisit.call
        val callArgs = call.arguments

        if (!call.isSwap()) return null

        val scope = CoroutineScope(coroutineContext)
        val multiLocationConverter = multiLocationConverterFactory.defaultAsync(chain, scope)

        val path = bindList(callArgs["path"], ::bindMultiLocation)
        val assetIn = multiLocationConverter.toChainAsset(path.first()) ?: return null
        val assetOut = multiLocationConverter.toChainAsset(path.last()) ?: return null

        val (amountIn, amountOut) = extrinsicVisit.extractSwapAmounts()

        val sendTo = bindAccountId(callArgs["send_to"])

        val fee = extrinsicVisit.extractFee(chain, multiLocationConverter)

        return RealtimeHistoryUpdate.Type.Swap(
            amountIn = ChainAssetWithAmount(assetIn, amountIn),
            amountOut = ChainAssetWithAmount(assetOut, amountOut),
            amountFee = fee,
            senderId = extrinsicVisit.origin,
            receiverId = sendTo
        )
    }

    private fun ExtrinsicVisit.extractSwapAmounts(): Pair<Balance, Balance> {
        // We check for custom fee usage from root extrinsic since `extrinsicVisit` will cut it out when nested calls are present
        val isCustomFeeTokenUsed = rootExtrinsic.events.assetTxFeePaidEvent() != null
        val allSwaps = events.findAllOfType(Modules.ASSET_CONVERSION, "SwapExecuted")

        val swapExecutedEvent = when {
            !success -> null // we wont be able to extract swap from event

            isCustomFeeTokenUsed -> {
                // Swaps with custom fee token produce up to free SwapExecuted events, in the following order:
                // SwapExecuted (Swap custom token fee to native token) - always present
                // SwapExecuted (Real swap) - always present
                // SwapExecuted (Refund remaining fee back to custom token)
                // So we need to take the middle one

                allSwaps.getOrNull(1)
            }

            else -> {
                // Only one swap is possible in case
                allSwaps.firstOrNull()
            }
        }

        return when {
            // successful swap, extract from event
            swapExecutedEvent != null -> {
                val (_, _, amountIn, amountOut) = swapExecutedEvent.arguments

                bindNumber(amountIn) to bindNumber(amountOut)
            }

            // failed swap, extract from call args
            call.function.name == "swap_exact_tokens_for_tokens" -> {
                val amountIn = bindNumber(call.arguments["amount_in"])
                val amountOutMin = bindNumber(call.arguments["amount_out_min"])

                amountIn to amountOutMin
            }

            call.function.name == "swap_tokens_for_exact_tokens" -> {
                val amountOut = bindNumber(call.arguments["amount_out"])
                val amountInMax = bindNumber(call.arguments["amount_in_max"])

                amountInMax to amountOut
            }

            else -> error("Unknown call")
        }
    }

    private suspend fun ExtrinsicVisit.extractFee(
        chain: Chain,
        multiLocationConverter: MultiLocationConverter
    ): ChainAssetWithAmount {
        // We check for fee usage from root extrinsic since `extrinsicVisit` will cut it out when nested calls are present
        val assetFee = rootExtrinsic.events.assetFee(multiLocationConverter)
        if (assetFee != null) return assetFee

        val nativeFee = rootExtrinsic.events.requireNativeFee()
        return ChainAssetWithAmount(chain.commissionAsset, nativeFee)
    }

    private fun GenericCall.Instance.isSwap(): Boolean {
        return module.name == Modules.ASSET_CONVERSION &&
            function.name in calls
    }
}
