package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
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
    val match: Match
)

private enum class Match {
    NONE, INCLUSION, PREFIX, FULL;
}

private val Match.matchFound
    get() = this != Match.NONE
private val Match.isFullMatch
    get() = this == Match.FULL

class AssetSearchInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val currencyRepository: CurrencyRepository
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
            val selectedCurrency = currencyRepository.getSelectedCurrency()
            val chainsById = chainRegistry.chainsById.first()
            val filtered = assets.filterBy(query, chainsById)

            groupAndSortAssetsByNetwork(filtered, chainsById, selectedCurrency)
        }
    }

    // O(N * logN)
    private fun List<Asset>.filterBy(query: String, chainsById: Map<String, Chain>): List<Asset> {
        if (query.isEmpty()) return this

        val searchResultsFromTokens = map {
            SearchResult(
                item = it,
                match = it.token.configuration.symbol match query
            )
        }
        val anyMatchFromTokens = searchResultsFromTokens.mapNotNull { searchResult ->
            searchResult.item.takeIf { searchResult.match.matchFound }
        }

        val allFullMatchesFromTokens = searchResultsFromTokens.filter { it.match.isFullMatch }
        if (allFullMatchesFromTokens.isNotEmpty()) {
            return anyMatchFromTokens
        }

        val foundChainIds = chainsById.values.filter { it.name inclusionMatch query }
            .map(Chain::id)
            .toSet()

        val fromChainSearch = filter { it.token.configuration.chainId in foundChainIds }

        return (anyMatchFromTokens + fromChainSearch).distinct()
    }

    private infix fun String.match(query: String): Match = when {
        fullMatch(query) -> Match.FULL
        prefixMatch(prefix = query) -> Match.PREFIX
        inclusionMatch(inclusion = query) -> Match.INCLUSION
        else -> Match.NONE
    }

    @OptIn(ExperimentalStdlibApi::class)
    private infix fun String.fullMatch(other: String) = lowercase() == other.lowercase()

    @OptIn(ExperimentalStdlibApi::class)
    private infix fun String.prefixMatch(prefix: String) = lowercase().startsWith(prefix.lowercase())

    @OptIn(ExperimentalStdlibApi::class)
    private infix fun String.inclusionMatch(inclusion: String) = inclusion.lowercase() in lowercase()
}
