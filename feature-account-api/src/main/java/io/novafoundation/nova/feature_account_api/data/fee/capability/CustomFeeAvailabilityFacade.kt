package io.novafoundation.nova.feature_account_api.data.fee.capability

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

interface CustomFeeCapability {

    /**
     * Implementations should expect `asset` to be non-utility asset,
     * e.g. they don't need to additionally check whether asset is utility or not
     */
    suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean
}

interface FastLookupCustomFeeCapability {

    fun canPayFeeInNonUtilityToken(chainAssetId: ChainAssetId): Boolean
}

interface CustomFeeCapabilityFacade {

    suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset, customFeeCapability: CustomFeeCapability): Boolean
}
