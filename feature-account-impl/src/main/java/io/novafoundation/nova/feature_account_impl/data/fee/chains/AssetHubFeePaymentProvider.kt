package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.chains.CustomOrNativeFeePaymentProvider
import io.novafoundation.nova.feature_account_api.data.fee.types.NativeFeePayment
import io.novafoundation.nova.feature_account_api.data.fee.types.assetHub.findChargeAssetTxPayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetConversionFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetHubFastLookupFeeCapability
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetHubFeePaymentAssetsFetcherFactory
import io.novafoundation.nova.feature_xcm_api.converter.LocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.versions.detector.XcmVersionDetector
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@FeatureScope
class AssetHubFeePaymentProviderFactory @Inject constructor(
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val multiLocationConverterFactory: LocationConverterFactory,
    private val assetHubFeePaymentAssetsFetcher: AssetHubFeePaymentAssetsFetcherFactory,
    private val chainRegistry: ChainRegistry,
    private val xcmVersionDetector: XcmVersionDetector
) {

    suspend fun create(chainId: ChainId): AssetHubFeePaymentProvider {
        val chain = chainRegistry.getChain(chainId)
        return AssetHubFeePaymentProvider(
            chain = chain,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            multiLocationConverterFactory = multiLocationConverterFactory,
            assetHubFeePaymentAssetsFetcher = assetHubFeePaymentAssetsFetcher,
            xcmVersionDetector = xcmVersionDetector
        )
    }
}

class AssetHubFeePaymentProvider(
    private val chain: Chain,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val multiLocationConverterFactory: LocationConverterFactory,
    private val assetHubFeePaymentAssetsFetcher: AssetHubFeePaymentAssetsFetcherFactory,
    private val xcmVersionDetector: XcmVersionDetector
) : CustomOrNativeFeePaymentProvider() {

    private val multiLocationConverter = SingleValueCache {
        multiLocationConverterFactory.createAssetLocationConverter()
    }

    override suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment {
        return AssetConversionFeePayment(
            paymentAsset = customFeeAsset,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            multiLocationConverter = multiLocationConverter(),
            assetHubFeePaymentAssetsFetcher = assetHubFeePaymentAssetsFetcher.create(chain, multiLocationConverter()),
            xcmVersionDetector = xcmVersionDetector
        )
    }

    override suspend fun detectFeePaymentFromExtrinsic(extrinsic: SendableExtrinsic): FeePayment {
        val feePaymentAsset = extrinsic.extrinsic.detectFeePaymentAsset() ?: return NativeFeePayment()

        return AssetConversionFeePayment(
            paymentAsset = feePaymentAsset,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            multiLocationConverter = multiLocationConverter(),
            assetHubFeePaymentAssetsFetcher = assetHubFeePaymentAssetsFetcher.create(chain, multiLocationConverter()),
            xcmVersionDetector = xcmVersionDetector
        )
    }

    override suspend fun fastLookupCustomFeeCapability(): Result<FastLookupCustomFeeCapability> {
        return runCatching {
            val fetcher = assetHubFeePaymentAssetsFetcher.create(chain, multiLocationConverter())
            AssetHubFastLookupFeeCapability(fetcher.fetchAvailablePaymentAssets())
        }
    }

    private suspend fun Extrinsic.Instance.detectFeePaymentAsset(): Chain.Asset? {
        val assetId = findChargeAssetTxPayment()?.assetId ?: return null
        return multiLocationConverter().chainAssetFromRelativeLocation(assetId, pointOfView = chain)
    }
}
