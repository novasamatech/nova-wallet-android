package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.bindMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvent
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.nativeFee
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

class AssetConversionSwapExtractor(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
) : SubstrateRealtimeOperationFetcher.Extractor {

    private val calls = listOf("swap_exact_tokens_for_tokens", "swap_tokens_for_exact_tokens")

    override suspend fun extractRealtimeHistoryUpdates(
        extrinsic: ExtrinsicWithEvents,
        chain: Chain,
        chainAsset: Chain.Asset
    ): RealtimeHistoryUpdate.Type? {
        val call = extrinsic.extrinsic.call
        val callArgs = call.arguments

        if (!call.isSwap()) return null

        val scope = CoroutineScope(coroutineContext)
        val multiLocationConverter = multiLocationConverterFactory.default(chain, scope)

        val path = bindList(callArgs["path"], ::bindMultiLocation)
        val assetIn = multiLocationConverter.toChainAsset(path.first()) ?: return null
        val assetOut = multiLocationConverter.toChainAsset(path.last()) ?: return null

        val (amountIn, amountOut) = extrinsic.extractSwapAmounts()

        val who = bindAccountIdentifier(extrinsic.extrinsic.signature!!.accountIdentifier)
        val sendTo = bindAccountId(callArgs["send_to"])

        val fee = extrinsic.extractFee(chain, multiLocationConverter)

        return RealtimeHistoryUpdate.Type.Swap(
            amountIn = ChainAssetWithAmount(assetIn, amountIn),
            amountOut = ChainAssetWithAmount(assetOut, amountOut),
            amountFee = fee,
            senderId = who,
            receiverId = sendTo
        )
    }

    private fun ExtrinsicWithEvents.extractSwapAmounts(): Pair<Balance, Balance> {
        val swapExecutedEvent = findEvent(Modules.ASSET_CONVERSION, "SwapExecuted")

        return when {
            // successful swap, extract from event
            swapExecutedEvent != null -> {
                val (_, _, _, amountIn, amountOut) = swapExecutedEvent.arguments

                bindNumber(amountIn) to bindNumber(amountOut)
            }

            // failed swap, extract from call args
            extrinsic.call.function.name == "swap_exact_tokens_for_tokens" -> {
                val amountIn = bindNumber(extrinsic.call.arguments["amount_in"])
                val amountOutMin = bindNumber(extrinsic.call.arguments["amount_out_min"])

                amountIn to amountOutMin
            }

            extrinsic.call.function.name == "swap_tokens_for_exact_tokens" -> {
                val amountOut = bindNumber(extrinsic.call.arguments["amount_out"])
                val amountInMax = bindNumber(extrinsic.call.arguments["amount_in_max"])

                amountInMax to amountOut
            }

            else -> error("Unknown call")
        }
    }

    private suspend fun ExtrinsicWithEvents.extractFee(
        chain: Chain,
        multiLocationConverter: MultiLocationConverter
    ) : ChainAssetWithAmount {
        val assetFee = assetFee(multiLocationConverter)
        if (assetFee != null) return assetFee

        val nativeFee = nativeFee()!!
        return ChainAssetWithAmount(chain.commissionAsset, nativeFee)
    }

    private fun GenericCall.Instance.isSwap(): Boolean {
        return module.name == Modules.ASSET_CONVERSION
            && function.name in calls
    }
}
