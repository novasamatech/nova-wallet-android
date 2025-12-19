package io.novafoundation.nova.runtime.ext

import io.novafoundation.nova.common.utils.TokenPriorityStore
import io.novafoundation.nova.common.utils.TokenSymbol
import kotlinx.coroutines.flow.Flow

class TokenSortingProvider(private val tokenPriorityStore: TokenPriorityStore) {

    fun tokenDisplayPriorityFlow(): Flow<Map<TokenSymbol, Int>> {
        return tokenPriorityStore.tokenSortingFlow()
    }
}

val TokenSymbol.alphabeticalOrder
    get() = value

fun <K> TokenSymbol.Companion.defaultComparator(
    displayPriority: (K) -> Int?,
    symbolExtractor: (K) -> TokenSymbol
): Comparator<K> = compareBy<K> { displayPriority(it) ?: Int.MAX_VALUE }
    .thenBy { symbolExtractor(it).alphabeticalOrder }
