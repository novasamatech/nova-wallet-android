package io.novafoundation.nova.feature_account_impl.data.fee.types

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.network.hydration.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_account_api.data.network.hydration.setFeeCurrency
import io.novafoundation.nova.feature_account_api.data.network.hydration.toOnChainIdOrThrow
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

internal class HydrationConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val chainRegistry: ChainRegistry,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
) : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        val baseCall = extrinsicBuilder.getCall()
        extrinsicBuilder.resetCalls()

        extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.toOnChainIdOrThrow(paymentAsset))
        extrinsicBuilder.call(baseCall)
        extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.systemAssetId)
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        val quote = multiChainRuntimeCallsApi.forChain(paymentAsset.chainId).convertNativeFee(nativeFee.amount)
        requireNotNull(quote) {
            Log.e(LOG_TAG, "Quote for ${paymentAsset.symbol} fee was null")

            "Failed to calculate fee in ${paymentAsset.symbol}"
        }

        return SubstrateFee(amount = quote, submissionOrigin = nativeFee.submissionOrigin, assetId = paymentAsset.fullId)
    }
}
