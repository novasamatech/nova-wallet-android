package io.novafoundation.nova.feature_account_impl.data.fee.chains

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.chains.CustomOrNativeFeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.types.NativeFeePayment
import io.novafoundation.nova.feature_account_api.data.fee.types.assetHub.findChargeAssetTxPayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetConversionFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetHubFastLookupFeeCapability
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetHubFeePaymentAssetsFetcherFactory
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope

class AssetHubFeePaymentProvider @AssistedInject constructor(
    @Assisted override val chain: Chain,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val assetHubFeePaymentAssetsFetcher: AssetHubFeePaymentAssetsFetcherFactory,
    private val xcmVersionDetector: XcmVersionDetector
) : CustomOrNativeFeePaymentProvider() {

    @AssistedFactory
    interface Factory {

        fun create(chain: Chain): AssetHubFeePaymentProvider
    }

    override suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment {
        val multiLocationConverter = multiLocationConverterFactory.defaultSync(chain)

        return AssetConversionFeePayment(
            paymentAsset = customFeeAsset,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            multiLocationConverter = multiLocationConverter,
            xcmVersionDetector = xcmVersionDetector
        )
    }

    override suspend fun canPayFeeInNonUtilityToken(customFeeAsset: Chain.Asset): Result<Boolean> {
        // Asset hub does not support per-asset optimized query
        return fastLookupCustomFeeCapability()
            .map { it.canPayFeeInNonUtilityToken(customFeeAsset.id) }
    }

    override suspend fun detectFeePaymentFromExtrinsic(extrinsic: SendableExtrinsic): FeePayment {
        val multiLocationConverter = multiLocationConverterFactory.defaultSync(chain)
        val feePaymentAsset = extrinsic.extrinsic.detectFeePaymentAsset(multiLocationConverter) ?: return NativeFeePayment()

        return AssetConversionFeePayment(
            paymentAsset = feePaymentAsset,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            multiLocationConverter = multiLocationConverter,
            xcmVersionDetector = xcmVersionDetector
        )
    }

    override suspend fun fastLookupCustomFeeCapability(): Result<FastLookupCustomFeeCapability> {
        return runCatching {
            val fetcher = assetHubFeePaymentAssetsFetcher.create(chain)
            AssetHubFastLookupFeeCapability(fetcher.fetchAvailablePaymentAssets())
        }
    }

    private suspend fun Extrinsic.Instance.detectFeePaymentAsset(multiLocationConverter: MultiLocationConverter): Chain.Asset? {
        val assetId = findChargeAssetTxPayment()?.assetId ?: return null
        return multiLocationConverter.toChainAsset(assetId)
    }
}
