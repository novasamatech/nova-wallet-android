package io.novafoundation.nova.common.utils

import android.text.SpannableStringBuilder

const val LEFT_TO_RIGHT_ISOLATE = '⁦'
const val POP_DIRECTIONAL_ISOLATE = '⁩'

/**
 * Wraps the text into a left-to-right isolate (LRI ... PDI), so it keeps its own order next to right-to-left text
 * and does not decide the paragraph direction by itself.
 *
 * Use it for amounts and rates that may contain a right-to-left symbol (e.g. Bittensor subnet symbols like "ش"):
 * a leading LRM (U+200E) only affects the neighbouring neutral characters, so "1 ش ≈ 0.067 TAO" is still rendered
 * as "TAO 0.067 ≈ ش 1" once the first strong character is right-to-left.
 */
fun String.ltrIsolated(): String = "$LEFT_TO_RIGHT_ISOLATE$this$POP_DIRECTIONAL_ISOLATE"

/**
 * Same as [String.ltrIsolated], keeping the spans of a styled text (e.g. fraction styling of formatted amounts).
 */
fun CharSequence.ltrIsolated(): CharSequence = when (this) {
    is String -> ltrIsolated()
    else -> SpannableStringBuilder().append(LEFT_TO_RIGHT_ISOLATE).append(this).append(POP_DIRECTIONAL_ISOLATE)
}

/**
 * "1 X ≈ Y Z" where both sides and the whole rate are isolated, so the rate reads left to right whatever symbols it contains.
 */
fun ltrIsolatedRate(oneIn: CharSequence, rateOut: CharSequence): String {
    return "${oneIn.toString().ltrIsolated()} ≈ ${rateOut.toString().ltrIsolated()}".ltrIsolated()
}
