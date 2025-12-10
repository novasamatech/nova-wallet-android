package io.novafoundation.nova.common.utils

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_TOKEN_SORTING = "PREFS_TOKEN_SORTING"

interface TokenPriorityStore {
    fun setTokenSorting(sorting: Map<TokenSymbol, Int>)

    fun getTokenSorting(): Map<TokenSymbol, Int>

    fun tokenSortingFlow(): Flow<Map<TokenSymbol, Int>>
}

class RealTokenPriorityStore(val preferences: Preferences) : TokenPriorityStore {
    override fun setTokenSorting(sorting: Map<TokenSymbol, Int>) {
        val sortingSet = sorting.map { (symbol, priority) -> "${symbol.value}:$priority" }
            .toSet()
        preferences.putStringSet(PREFS_TOKEN_SORTING, sortingSet)
    }

    override fun getTokenSorting(): Map<TokenSymbol, Int> {
        val sortingSet = preferences.getStringSet(PREFS_TOKEN_SORTING)
        return sortingSet.mapFromSet()
    }

    override fun tokenSortingFlow(): Flow<Map<TokenSymbol, Int>> {
        return preferences.stringSetFlow(PREFS_TOKEN_SORTING)
            .map { it?.mapFromSet() ?: emptyMap() }
    }

    private fun Set<String>.mapFromSet(): Map<TokenSymbol, Int> {
        return associate {
            val (symbol, priority) = it.split(":")
            TokenSymbol(symbol) to priority.toInt()
        }
    }
}
