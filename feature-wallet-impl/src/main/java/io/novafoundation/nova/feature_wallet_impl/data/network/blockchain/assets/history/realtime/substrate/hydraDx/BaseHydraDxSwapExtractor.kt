package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate.hydraDx

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.api.ExtrinsicVisit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findLastEvent
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.requireNativeFee
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

abstract class BaseHydraDxSwapExtractor(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : SubstrateRealtimeOperationFetcher.Extractor {

    abstract fun isSwap(call: GenericCall.Instance): Boolean

    protected abstract fun ExtrinsicVisit.extractSwapArgs(): SwapArgs

    override suspend fun extractRealtimeHistoryUpdates(
        extrinsicVisit: ExtrinsicVisit,
        chain: Chain,
        chainAsset: Chain.Asset
    ): RealtimeHistoryUpdate.Type? {
        if (!isSwap(extrinsicVisit.call)) return null

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

    private suspend fun ExtrinsicVisit.extractFee(chain: Chain): ChainAssetWithAmount {
        val feeDepositEvent = rootExtrinsic.events.findLastEvent(Modules.CURRENCIES, "Deposited") ?: return nativeFee(chain)

        val (currencyIdRaw, _, amountRaw) = feeDepositEvent.arguments
        val currencyId = bindNumber(currencyIdRaw)

        val feeAsset = hydraDxAssetIdConverter.toChainAssetOrNull(chain, currencyId) ?: return nativeFee(chain)

        return ChainAssetWithAmount(feeAsset, bindNumber(amountRaw))
    }

    private fun ExtrinsicVisit.nativeFee(chain: Chain): ChainAssetWithAmount {
        return ChainAssetWithAmount(chain.utilityAsset, rootExtrinsic.events.requireNativeFee())
    }

    protected data class SwapArgs(
        val assetIn: HydraDxAssetId,
        val assetOut: HydraDxAssetId,
        val amountIn: HydraDxAssetId,
        val amountOut: HydraDxAssetId
    )
}
