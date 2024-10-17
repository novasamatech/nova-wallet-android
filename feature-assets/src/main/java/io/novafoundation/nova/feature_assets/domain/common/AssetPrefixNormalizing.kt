package io.novafoundation.nova.feature_assets.domain.common

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.asTokenSymbol

/**
 * Some of tokens may have a prefix but may still be original token (like xcDOT, xcHDX)
 * For some cases we need to normalize token symbol to original format
 */

private val normalizingPrefixList: List<String>
    get() = listOf("xc")

fun normalizeTokenSymbol(tokenSymbol: TokenSymbol): TokenSymbol {
    return normalizeTokenSymbol(tokenSymbol.value).asTokenSymbol()
}

fun normalizeTokenSymbol(symbol: String): String {
    normalizingPrefixList.forEach { prefix ->
        if (symbol.startsWith(prefix)) {
            return symbol.removePrefix(prefix)
        }
    }

    return symbol
}
