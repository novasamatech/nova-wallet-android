package io.novafoundation.nova.feature_account_api.data.fee.capability

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

interface FastLookupCustomFeeCapability {

    val nonUtilityFeeCapableTokens: Set<ChainAssetId>

    fun canPayFeeInNonUtilityToken(chainAssetId: ChainAssetId): Boolean {
        return chainAssetId in nonUtilityFeeCapableTokens
    }
}

interface CustomFeeCapabilityFacade {

    suspend fun canPayFeeInCurrency(currency: FeePaymentCurrency): Boolean

    /**
     * Whether fee payment in custom assets is not possible at all in the current environment
     * This check is also accounted for internally in [canPayFeeInNonUtilityToken]
     * but can be used separately for optimizing bulk checks
     */
    suspend fun hasGlobalFeePaymentRestrictions(): Boolean
}
