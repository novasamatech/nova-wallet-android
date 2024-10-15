package io.novafoundation.nova.common.utils

import android.os.Parcelable
import io.novafoundation.nova.common.utils.formatting.format
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class TokenSymbol(val value: String) : Parcelable {

    override fun toString() = value
}

fun String.asTokenSymbol() = TokenSymbol(this)

fun BigDecimal.formatTokenAmount(tokenSymbol: TokenSymbol, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return format(roundingMode).withTokenSymbol(tokenSymbol)
}

fun String.withTokenSymbol(tokenSymbol: TokenSymbol): String {
    return "$this ${tokenSymbol.value}"
}
