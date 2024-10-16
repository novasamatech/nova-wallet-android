package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

class AssetHubFastLookupFeeCapability(
    private val assetsFetcher: AssetHubFeePaymentAssetsFetcher,
): FastLookupCustomFeeCapability {

    private var cachedAssets: Set<Int>? = null

    override suspend fun canPayFeeInNonUtilityToken(chainAssetId: ChainAssetId): Boolean {
        return chainAssetId in getAllowedFeePaymentAssets()
    }

    private suspend fun getAllowedFeePaymentAssets(): Set<Int> {
        // We are not guarding it with mutex to make it more optimized and avoid synchronisation overhead
        if (cachedAssets == null) {
            cachedAssets = assetsFetcher.fetchAvailablePaymentAssets()
        }

        return cachedAssets!!
    }
}
