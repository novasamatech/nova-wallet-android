package io.novafoundation.nova.feature_account_api.data.fee.capability

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface CustomFeeCapability {

    /**
     * Implementations should expect `asset` to be non-utility asset,
     * e.g. they don't need to additionally check whether asset is utility or not
     * They can also expect this method is called only when asset is present in [AssetExchange.availableDirectSwapConnections]
     */
    suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean
}

interface CustomFeeCapabilityFacade {

    suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset, customFeeCapability: CustomFeeCapability): Boolean
}
