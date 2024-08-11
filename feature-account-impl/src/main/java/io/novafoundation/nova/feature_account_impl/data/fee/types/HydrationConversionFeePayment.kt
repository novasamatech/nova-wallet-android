package io.novafoundation.nova.feature_account_impl.data.fee.types

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_impl.data.fee.utils.HydraDxQuoteSharedComputation
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.data.network.setFeeCurrency
import io.novafoundation.nova.feature_swap_core.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope

internal class HydrationConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val chainRegistry: ChainRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation,
    private val coroutineScope: CoroutineScope
) : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        val baseCall = extrinsicBuilder.getCall()
        extrinsicBuilder.resetCalls()

        extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.toOnChainIdOrThrow(paymentAsset))
        extrinsicBuilder.call(baseCall)
        extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.systemAssetId)
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        val chain = chainRegistry.getChain(paymentAsset.chainId)
        val fromAsset = chain.commissionAsset
        val quote = hydraDxQuoteSharedComputation.quote(chain, fromAsset = fromAsset, toAsset = paymentAsset, coroutineScope)
        return SubstrateFee(quote.quote, nativeFee.submissionOrigin, paymentAsset.fullId)
    }

    override suspend fun availableCustomFeeAssets(): List<Chain.Asset> {
        val chain = chainRegistry.getChain(paymentAsset.chainId)

        val allSwapDirections = hydraDxQuoteSharedComputation.directions(chain, coroutineScope)

        val fromAsset = chain.commissionAsset
        val cimmissionAssetDirections = allSwapDirections.adjacencyList[fromAsset.fullId] ?: emptyList()
        return cimmissionAssetDirections.map { chainRegistry.asset(it.direction.to) }
    }
}
