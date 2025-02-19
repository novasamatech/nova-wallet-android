package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.assetConversionAssetIdType
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.toMultiLocationOrThrow
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.feature_xcm_api.versions.orDefault
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.SignedExtensionValue
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
        return extrinsicBuilder.assetTxPayment(encodableAssetId(xcmVersion))
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
        return RelativeMultiLocation(
            parents = 1,
            interior = Interior.Here
        ).toEncodableInstance(xcmVersion)
    }

    private fun ExtrinsicBuilder.assetTxPayment(assetId: Any?, tip: BigInteger = BigInteger.ZERO) {
        val extensionValue = assetTxPaymentPayload(assetId, tip)

        signedExtension(
            id = "ChargeAssetTxPayment",
            value = SignedExtensionValue(includedInExtrinsic = extensionValue)
        )
    }

    private fun assetTxPaymentPayload(assetId: Any?, tip: BigInteger = BigInteger.ZERO): Any {
        return structOf(
            "tip" to tip,
            "assetId" to assetId
        )
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
