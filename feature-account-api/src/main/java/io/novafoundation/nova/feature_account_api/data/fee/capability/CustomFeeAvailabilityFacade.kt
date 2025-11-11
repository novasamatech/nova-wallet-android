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

    val nonUtilityFeeCapableTokens: Set<ChainAssetId>

    fun canPayFeeInNonUtilityToken(chainAssetId: ChainAssetId): Boolean {
        return chainAssetId in nonUtilityFeeCapableTokens
    }
}

interface CustomFeeCapabilityFacade {

    suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset, customFeeCapability: CustomFeeCapability): Boolean

    /**
     * Whether fee payment in custom assets is not possible at all in the current environment
     * This check is also accounted for internally in [canPayFeeInNonUtilityToken]
     * but can be used separately for optimizing bulk checks
     */
    suspend fun hasGlobalFeePaymentRestrictions(): Boolean
}
