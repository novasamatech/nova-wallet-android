package io.novafoundation.nova.feature_assets.domain.assets.search

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.data.fee.fastLookupCustomFeeCapabilityOrDefault
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.getAssetOrThrow
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold

class AssetSearchUseCase(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val swapService: SwapService,
    private val computationalCache: ComputationalCache,
    private val feePaymentRegistry: FeePaymentProviderRegistry,
    private val feePaymentFacade: CustomFeeCapabilityFacade,
    private val assetSourceRegistry: AssetSourceRegistry,
) {

    companion object {

        private const val GIFT_ASSETS_CACHE = "AssetSearchUseCase.GIFT_ASSETS_CACHE"
    }

    fun filteredAssetFlow(filterFlow: Flow<AssetSearchFilter?>): Flow<List<Asset>> {
        val assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.syncedAssetsFlow(it.id) }

        return combine(assetsFlow, filterFlow) { assets, filter ->
            if (filter == null) {
                assets
            } else {
                assets.filter { filter(it) }
            }
        }
    }

    fun filterAssetsByQuery(query: String, assets: List<Asset>, chainsById: ChainsById): List<Asset> {
        return assets.searchTokens(
            query = query,
            chainsById = chainsById,
            tokenSymbol = { it.token.configuration.symbol.value },
            relevantToChains = { asset, chainIds -> asset.token.configuration.chainId in chainIds }
        )
    }

    fun getAvailableSwapAssets(asset: FullChainAssetId?, coroutineScope: CoroutineScope): Flow<Set<FullChainAssetId>> {
        return flowOfAll {
            val chainAsset = asset?.let { chainRegistry.asset(it) }

            if (chainAsset == null) {
                swapService.assetsAvailableForSwap(coroutineScope)
            } else {
                swapService.availableSwapDirectionsFor(chainAsset, coroutineScope)
            }
        }
    }

    fun getAvailableGiftAssets(coroutineScope: CoroutineScope): Flow<Set<FullChainAssetId>> {
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

    private suspend fun ChainRegistry.allNativeAssetIds(): Set<FullChainAssetId> {
        return currentChains.first().mapToSet { it.utilityAsset.fullId }
    }
}
