package io.novafoundation.nova.feature_assets.presentation.transaction.filter

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

private const val FILTERS__PROVIDER_TAG = "HistoryFiltersProvider"

/**
 * Factory ensures that [HistoryFiltersProvider] scope will be limited to the current flow of screens
 */
class HistoryFiltersProviderFactory(
    private val computationalCache: ComputationalCache,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun get(
        scope: CoroutineScope,
        chainId: ChainId,
        chainAssetId: ChainAssetId,
    ): HistoryFiltersProvider {
        val key = "$FILTERS__PROVIDER_TAG:$chainId:$chainAssetId"

        return computationalCache.useCache(key, scope) {
            val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
            val source = assetSourceRegistry.sourceFor(chainAsset)
            val allAvailableFilters = source.history.availableOperationFilters(chain, chainAsset)

            HistoryFiltersProvider(allAvailableFilters)
        }
    }
}

class HistoryFiltersProvider(val allAvailableFilters: Set<TransactionFilter>) {

    val defaultFilters = allAvailableFilters

    private val customFiltersFlow = MutableStateFlow(allAvailableFilters)

    fun currentFilters() = customFiltersFlow.value

    fun filtersFlow(): Flow<Set<TransactionFilter>> = customFiltersFlow

    fun setCustomFilters(filters: Set<TransactionFilter>) {
        customFiltersFlow.value = filters
    }
}
