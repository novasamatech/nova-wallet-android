package io.novafoundation.nova.common.utils

import io.novafoundation.nova.common.utils.formatting.format
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class TokenSymbol(val value: String) {

    companion object; // extensions

    override fun toString() = value
}

fun String.asTokenSymbol() = TokenSymbol(this)

fun BigDecimal.formatTokenAmount(roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return format(roundingMode)
}

fun BigDecimal.formatTokenAmount(tokenSymbol: TokenSymbol, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return format(roundingMode).withTokenSymbol(tokenSymbol)
}

fun String.withTokenSymbol(tokenSymbol: TokenSymbol): String {
    return "$this ${tokenSymbol.value}"
}
