package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

private class SearchResult(
    val item: Asset,
    val fullMatch: Boolean,
    val prefixMatch: Boolean
)

class AssetSearchInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) {

    fun searchAssetsFlow(
        queryFlow: Flow<String>
    ): Flow<Map<AssetGroup, List<Asset>>> {
        val assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.assetsFlow(it.id) }

        return combine(
            assetsFlow,
            queryFlow
        ) { assets, query ->
            val chainsById = chainRegistry.chainsById.first()
            val filtered = assets.filterBy(query, chainsById)

            groupAndSortAssetsByNetwork(filtered, chainsById)
        }
    }

    // O(N * logN)
    private fun List<Asset>.filterBy(query: String, chainsById: Map<String, Chain>): List<Asset> {
        if (query.isEmpty()) return this

        val searchResultsFromTokens = map {
            SearchResult(
                item = it,
                fullMatch = it.token.configuration.symbol fullMatch query,
                prefixMatch = it.token.configuration.symbol prefixMatch query
            )
        }
        val allFullMatchesFromTokens = searchResultsFromTokens.filter { it.fullMatch }
        if (allFullMatchesFromTokens.isNotEmpty()) {
            return allFullMatchesFromTokens.map(SearchResult::item)
        }

        val foundChainIds = chainsById.values.filter { it.name inclusionMatch query }
            .map(Chain::id)
            .toSet()

        val fromTokenSearch = searchResultsFromTokens.mapNotNull { searchResult ->
            searchResult.item.takeIf { searchResult.prefixMatch }
        }
        val fromChainSearch = filter { it.token.configuration.chainId in foundChainIds }

        return (fromTokenSearch + fromChainSearch).distinct()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private infix fun String.fullMatch(other: String) = lowercase() == other.lowercase()

    @OptIn(ExperimentalStdlibApi::class)
    private infix fun String.prefixMatch(prefix: String) = lowercase().startsWith(prefix.lowercase())

    @OptIn(ExperimentalStdlibApi::class)
    private infix fun String.inclusionMatch(inclusion: String) = inclusion.lowercase() in lowercase()
}
