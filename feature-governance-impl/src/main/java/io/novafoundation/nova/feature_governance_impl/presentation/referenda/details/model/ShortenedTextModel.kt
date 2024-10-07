package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model

import android.text.Spanned
import android.text.TextUtils
import android.widget.TextView
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.ReadMoreView

private const val ELLIPSIS = "..."

interface CharacterLimit {

    val limit: Int
}

enum class DefaultCharacterLimit(override val limit: Int) : CharacterLimit {

    SHORT_PARAGRAPH(180)
}

class ShortenedTextModel private constructor(val shortenedText: CharSequence, val hasMore: Boolean) {

    companion object {
        fun from(text: CharSequence, characterLimit: CharacterLimit): ShortenedTextModel {
            require(characterLimit.limit >= ELLIPSIS.length)

            return if (text.length > characterLimit.limit) {
                val shortened = text.subSequence(0, characterLimit.limit - ELLIPSIS.length)
                val shortenedEllipsized = TextUtils.concat(shortened, ELLIPSIS)
                ShortenedTextModel(shortenedText = shortenedEllipsized, hasMore = true)
            } else {
                ShortenedTextModel(shortenedText = text, hasMore = false)
            }
        }
    }
}

fun ShortenedTextModel?.applyTo(textView: TextView, readMoreView: ReadMoreView, markwon: Markwon) {
    if (this == null) {
        textView.makeGone()
        readMoreView.makeGone()
        return
    }

    if (shortenedText is Spanned) {
        markwon.setParsedMarkdown(textView, shortenedText)
    } else {
        textView.text = shortenedText
    }

    textView.setVisible(true)
    readMoreView.setVisible(hasMore)
}
