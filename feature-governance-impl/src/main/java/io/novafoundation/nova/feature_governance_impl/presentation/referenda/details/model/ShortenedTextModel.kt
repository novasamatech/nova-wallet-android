package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model

private const val ELLIPSIS = "..."

class ShortenedTextModel private constructor(val shortenedText: String, val hasMore: Boolean) {

    companion object {
        fun from(text: String, characterLimit: Int): ShortenedTextModel {
            require(characterLimit >= ELLIPSIS.length)

            return if (text.length > characterLimit) {
                val shortened = text.substring(0, characterLimit - ELLIPSIS.length) + ELLIPSIS

                ShortenedTextModel(shortenedText = shortened, hasMore = true)
            } else {
                ShortenedTextModel(shortenedText = text, hasMore = false)
            }
        }
    }
}
