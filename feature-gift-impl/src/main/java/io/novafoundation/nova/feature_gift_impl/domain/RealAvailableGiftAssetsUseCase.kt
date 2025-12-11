package io.novafoundation.nova.feature_gift_impl.domain

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency.Asset.Companion.toFeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.data.fee.fastLookupCustomFeeCapabilityOrDefault
import io.novafoundation.nova.feature_gift_api.domain.AvailableGiftAssetsUseCase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.getAssetOrThrow
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.withContext

class RealAvailableGiftAssetsUseCase(
    private val chainRegistry: ChainRegistry,
    private val computationalCache: ComputationalCache,
    private val feePaymentRegistry: FeePaymentProviderRegistry,
    private val feePaymentFacade: CustomFeeCapabilityFacade,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AvailableGiftAssetsUseCase {

    companion object {

        private const val GIFT_ASSETS_CACHE = "AssetSearchUseCase.GIFT_ASSETS_CACHE"
    }

    override suspend fun isGiftsAvailable(chainAsset: Chain.Asset): Boolean {
        return withContext(Dispatchers.Default) {
            val canPayFee = feePaymentFacade.canPayFeeInCurrency(chainAsset.toFeePaymentCurrency())
            val isSelfSufficient = assetSourceRegistry.isSelfSufficientAsset(chainAsset)

            canPayFee && isSelfSufficient
        }
    }

    override fun getAvailableGiftAssets(coroutineScope: CoroutineScope): Flow<Set<FullChainAssetId>> {
        return computationalCache.useSharedFlow(GIFT_ASSETS_CACHE, coroutineScope) {
            flow {
                // Fast first emission - show all native assets
                emit(chainRegistry.allNativeAssetIds())

                if (feePaymentFacade.hasGlobalFeePaymentRestrictions()) return@flow

                // Then do the full scan - via slower fee capability check
                emitPerChainAvailableAssets()
            }
                .runningFold(emptySet<FullChainAssetId>()) { acc, newAssets -> if (newAssets.isEmpty()) acc else acc + newAssets }
                .distinctUntilChangedBy { it.size } // we are only adding so deduplication by size is enough
                .onEach { Log.d("AssetSearchUseCase", "# of assets available for gifts: ${it.size}") }
        }
    }

    private suspend fun ChainRegistry.allNativeAssetIds(): Set<FullChainAssetId> {
        return currentChains.first().mapToSet { it.utilityAsset.fullId }
    }

    context(FlowCollector<Set<FullChainAssetId>>)
    private suspend fun emitPerChainAvailableAssets() {
        val chains = chainRegistry.currentChains.first()
        chains.map { chain -> flowOf { collectAllAssetsAllowedForGiftsInChain(chain) } }
            .merge()
            .collect { emit(it) }
    }

    private suspend fun collectAllAssetsAllowedForGiftsInChain(chain: Chain): Set<FullChainAssetId> {
        val canBeUsedForFeePayment = feePaymentRegistry
            .providerFor(chain.id)
            .fastLookupCustomFeeCapabilityOrDefault()
            .nonUtilityFeeCapableTokens

        return canBeUsedForFeePayment.mapNotNullToSet {
            val asset = chain.getAssetOrThrow(it)
            val isSufficient = assetSourceRegistry.isSelfSufficientAsset(asset)
            if (isSufficient) {
                asset.fullId
            } else {
                null
            }
        }
    }
}
