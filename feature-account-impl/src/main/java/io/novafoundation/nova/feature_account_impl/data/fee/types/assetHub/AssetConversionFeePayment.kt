package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.assetConversionAssetIdType
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.types.assetHub.chargeAssetTxPayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersion
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersionDetector
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.toMultiLocationOrThrow
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.orDefault
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.toEncodableInstance
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

internal class AssetConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val multiLocationConverter: MultiLocationConverter,
    private val assetHubFeePaymentAssetsFetcher: AssetHubFeePaymentAssetsFetcher,
    private val xcmVersionDetector: XcmVersionDetector
) : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        val xcmVersion = detectAssetIdXcmVersion(extrinsicBuilder.runtime)
        return extrinsicBuilder.chargeAssetTxPayment(encodableAssetId(xcmVersion))
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        val quote = multiChainRuntimeCallsApi.forChain(paymentAsset.chainId).convertNativeFee(nativeFee.amount)
        requireNotNull(quote) {
            Log.e(LOG_TAG, "Quote for ${paymentAsset.symbol} fee was null")

            "Failed to calculate fee in ${paymentAsset.symbol}"
        }

        return SubstrateFee(amount = quote, submissionOrigin = nativeFee.submissionOrigin, asset = paymentAsset)
    }

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean {
        val availableFeeAssets = assetHubFeePaymentAssetsFetcher.fetchAvailablePaymentAssets()
        return chainAsset.id in availableFeeAssets
    }

    private suspend fun encodableAssetId(xcmVersion: XcmVersion): Any {
        return multiLocationConverter.toMultiLocationOrThrow(paymentAsset).toEncodableInstance(xcmVersion)
    }

    private fun encodableNativeAssetId(xcmVersion: XcmVersion): Any {
        return MultiLocation(
            parents = BigInteger.ONE,
            interior = MultiLocation.Interior.Here
        ).toEncodableInstance(xcmVersion)
    }

    private suspend fun RuntimeCallsApi.convertNativeFee(amount: BigInteger): BigInteger? {
        val xcmVersion = detectAssetIdXcmVersion(runtime)

        return call(
            section = "AssetConversionApi",
            method = "quote_price_tokens_for_exact_tokens",
            arguments = mapOf(
                "asset1" to encodableAssetId(xcmVersion),
                "asset2" to encodableNativeAssetId(xcmVersion),
                "amount" to amount,
                "include_fee" to true
            ),
            returnBinding = ::bindNumberOrNull
        )
    }

    private suspend fun detectAssetIdXcmVersion(runtime: RuntimeSnapshot): XcmVersion {
        val assetIdType = runtime.metadata.assetConversionAssetIdType()
        return xcmVersionDetector.detectMultiLocationVersion(paymentAsset.chainId, assetIdType).orDefault()
    }
}
