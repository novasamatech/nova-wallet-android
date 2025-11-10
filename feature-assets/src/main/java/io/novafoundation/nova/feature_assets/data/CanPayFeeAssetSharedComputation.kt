package io.novafoundation.nova.feature_assets.data

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.data.fee.toFeePaymentCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

interface CanPayFeeAssetSharedComputation {
    suspend fun canPayFeeInAsset(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): Boolean
}

class RealCanPayFeeAssetSharedComputation(
    private val computationalCache: ComputationalCache,
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry,
    private val customFeeCapabilityFacade: CustomFeeCapabilityFacade,
) : CanPayFeeAssetSharedComputation {

    override suspend fun canPayFeeInAsset(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): Boolean {
        if (chainAsset.isCommissionAsset) return true

        val key = "nonUtilityAssets:canPayFeeInAsset:${chainAsset.chainId}:${chainAsset.id}"
        return computationalCache.useCache(key, coroutineScope) {
            val feePaymentCurrency = chainAsset.toFeePaymentCurrency()

            val feePayment = feePaymentProviderRegistry.providerFor(chainAsset.chainId)
                .feePaymentFor(feePaymentCurrency, coroutineScope)

            customFeeCapabilityFacade.canPayFeeInNonUtilityToken(chainAsset, feePayment)
        }
    }
}
