package io.novafoundation.nova.feature_account_api.data.fee.capability

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface CustomFeeCapability {

    suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset): Boolean
}

interface CustomFeeCapabilityFacade {

    suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset, customFeeCapability: CustomFeeCapability): Boolean
}
