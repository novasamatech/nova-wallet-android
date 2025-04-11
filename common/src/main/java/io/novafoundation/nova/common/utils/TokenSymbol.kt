package io.novafoundation.nova.common.utils

import android.os.Parcelable
import io.novafoundation.nova.common.utils.formatting.format
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class TokenSymbol(val value: String) : Parcelable {

    companion object; // extensions

    override fun toString() = value
}

fun String.asTokenSymbol() = TokenSymbol(this)

fun BigDecimal.formatTokenAmount(roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return format(roundingMode)
}

fun BigDecimal.formatTokenAmount(
    tokenSymbol: TokenSymbol,
    roundingMode: RoundingMode = RoundingMode.FLOOR,
    includeAssetTicker: Boolean = true
): String {
    val formatted = format(roundingMode)

    return if (includeAssetTicker) {
        formatted.withTokenSymbol(tokenSymbol)
    } else {
        formatted
    }
}

fun String.withTokenSymbol(tokenSymbol: TokenSymbol): String {
    return "$this ${tokenSymbol.value}"
}
