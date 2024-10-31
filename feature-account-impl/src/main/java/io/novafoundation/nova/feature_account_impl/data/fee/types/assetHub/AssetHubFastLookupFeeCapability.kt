package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

class AssetHubFastLookupFeeCapability(
    private val allowedPaymentAssets: Set<Int>,
): FastLookupCustomFeeCapability {

    override fun canPayFeeInNonUtilityToken(chainAssetId: ChainAssetId): Boolean {
        return chainAssetId in allowedPaymentAssets
    }
}
