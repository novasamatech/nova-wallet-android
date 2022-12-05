package io.novafoundation.nova.feature_assets.domain.common

import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

private class SearchResult<T>(
    val item: T,
    val match: Match
)

private enum class Match {
    NONE, INCLUSION, PREFIX, FULL;
}

private val Match.matchFound
    get() = this != Match.NONE
private val Match.isFullMatch
    get() = this == Match.FULL

// O(N * logN)
fun <T> List<T>.searchTokens(
    query: String,
    chainsById: ChainsById,
    tokenSymbol: (T) -> String,
    relevantToChains: (T, Set<ChainId>) -> Boolean,
): List<T> {
    if (query.isEmpty()) return this

    val searchResultsFromTokens = map {
        SearchResult(
            item = it,
            match = tokenSymbol(it) match query
        )
    }
    val anyMatchFromTokens = searchResultsFromTokens.mapNotNull { searchResult ->
        searchResult.item.takeIf { searchResult.match.matchFound }
    }

    val allFullMatchesFromTokens = searchResultsFromTokens.filter { it.match.isFullMatch }
    if (allFullMatchesFromTokens.isNotEmpty()) {
        return anyMatchFromTokens
    }

    val foundChainIds = chainsById.values.mapNotNullToSet { chain ->
        chain.id.takeIf { chain.name inclusionMatch query }
    }

    val fromChainSearch = filter { relevantToChains(it, foundChainIds) }

    return (anyMatchFromTokens + fromChainSearch).distinct()
}

private infix fun String.match(query: String): Match = when {
    fullMatch(query) -> Match.FULL
    prefixMatch(prefix = query) -> Match.PREFIX
    inclusionMatch(inclusion = query) -> Match.INCLUSION
    else -> Match.NONE
}

private infix fun String.fullMatch(other: String) = lowercase() == other.lowercase()

private infix fun String.prefixMatch(prefix: String) = lowercase().startsWith(prefix.lowercase())

private infix fun String.inclusionMatch(inclusion: String) = inclusion.lowercase() in lowercase()
