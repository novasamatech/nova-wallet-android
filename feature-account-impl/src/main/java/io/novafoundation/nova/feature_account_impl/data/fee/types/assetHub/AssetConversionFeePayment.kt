package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.toMultiLocationOrThrow
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.toEncodableInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.BooleanType
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.SignedExtensionValue
import io.novasama.substrate_sdk_android.runtime.metadata.call
import io.novasama.substrate_sdk_android.runtime.metadata.module
import java.math.BigInteger

internal class AssetConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val multiLocationConverter: MultiLocationConverter,
    private val assetHubFeePaymentAssetsFetcher: AssetHubFeePaymentAssetsFetcher,
) : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        return extrinsicBuilder.assetTxPayment(encodableAssetId())
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

    private suspend fun encodableAssetId(): Any {
        return multiLocationConverter.toMultiLocationOrThrow(paymentAsset).toEncodableInstance()
    }

    private fun encodableNativeAssetId(): Any {
        return MultiLocation(
            parents = BigInteger.ONE,
            interior = MultiLocation.Interior.Here
        ).toEncodableInstance()
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
        val method = "quote_price_tokens_for_exact_tokens"

        val includeFee = true

        val multiLocationTypeName = runtime.metadata.assetIdTypeName()

        return call(
            section = "AssetConversionApi",
            method = method,
            arguments = listOf(
                encodableAssetId() to multiLocationTypeName,
                encodableNativeAssetId() to multiLocationTypeName,
                amount to "Balance",
                includeFee to BooleanType.name
            ),
            returnType = "Option<Balance>",
            returnBinding = ::bindNumberOrNull
        )
    }

    private fun RuntimeMetadata.assetIdTypeName(): String {
        val (assetIdArgument) = module(Modules.ASSET_CONVERSION).call("add_liquidity").arguments

        val assetIdType = assetIdArgument.type!!

        return assetIdType.name
    }
}