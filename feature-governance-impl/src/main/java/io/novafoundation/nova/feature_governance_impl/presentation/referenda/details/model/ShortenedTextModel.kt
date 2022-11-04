package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model

import android.text.TextUtils

private const val ELLIPSIS = "..."

class ShortenedTextModel private constructor(val shortenedText: CharSequence, val hasMore: Boolean) {

    companion object {
        fun from(text: CharSequence, characterLimit: Int): ShortenedTextModel {
            require(characterLimit >= ELLIPSIS.length)

            return if (text.length > characterLimit) {
                val shortened = text.subSequence(0, characterLimit - ELLIPSIS.length)
                val shortenedEllipsized = TextUtils.concat(shortened, ELLIPSIS)
                ShortenedTextModel(shortenedText = shortenedEllipsized, hasMore = true)
            } else {
                ShortenedTextModel(shortenedText = text, hasMore = false)
            }
        }
    }
}
