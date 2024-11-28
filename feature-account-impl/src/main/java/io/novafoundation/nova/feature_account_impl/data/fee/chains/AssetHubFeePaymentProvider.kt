package io.novafoundation.nova.feature_account_impl.data.fee.chains

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.chains.CustomOrNativeFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetConversionFeePayment
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetHubFastLookupFeeCapability
import io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub.AssetHubFeePaymentAssetsFetcherFactory
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersionDetector
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import kotlinx.coroutines.CoroutineScope

class AssetHubFeePaymentProviderFactory(
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
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
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val assetHubFeePaymentAssetsFetcher: AssetHubFeePaymentAssetsFetcherFactory,
    private val xcmVersionDetector: XcmVersionDetector
) : CustomOrNativeFeePaymentProvider() {

    override suspend fun feePaymentFor(customFeeAsset: Chain.Asset, coroutineScope: CoroutineScope?): FeePayment {
        val multiLocationConverter = multiLocationConverterFactory.defaultSync(chain)

        return AssetConversionFeePayment(
            paymentAsset = customFeeAsset,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            multiLocationConverter = multiLocationConverter,
            assetHubFeePaymentAssetsFetcher = assetHubFeePaymentAssetsFetcher.create(chain, multiLocationConverter),
            xcmVersionDetector = xcmVersionDetector
        )
    }

    override suspend fun fastLookupCustomFeeCapability(): Result<FastLookupCustomFeeCapability> {
        return runCatching {
            val fetcher = assetHubFeePaymentAssetsFetcher.create(chain)
            AssetHubFastLookupFeeCapability(fetcher.fetchAvailablePaymentAssets())
        }
    }
}
