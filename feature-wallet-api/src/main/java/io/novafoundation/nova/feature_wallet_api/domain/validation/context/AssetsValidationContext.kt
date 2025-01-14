package io.novafoundation.nova.feature_wallet_api.domain.validation.context

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/**
 * Abstraction over asset-related lookups that can be used across multiple validations in the same validation system
 * When constructing ValidationSystem, create an instance via [AssetsValidationContext.Factory] and pass as the dependency to the necessary validation
 *
 * Implementation may cache certain results to avoid duplicated network or db lookups
 */
interface AssetsValidationContext {

    interface Factory {

        fun create(): AssetsValidationContext
    }

    suspend fun getAsset(chainAsset: Chain.Asset): Asset

    suspend fun getAsset(chainAssetId: FullChainAssetId): Asset

    suspend fun getExistentialDeposit(chainAssetId: FullChainAssetId): Balance

    suspend fun isAssetSufficient(chainAsset: Chain.Asset): Boolean

    suspend fun canTotalDropBelowEd(chainAsset: Chain.Asset): Boolean
}

suspend fun AssetsValidationContext.getExistentialDeposit(chainAsset: Chain.Asset): Balance {
    return getExistentialDeposit(chainAsset.fullId)
}
