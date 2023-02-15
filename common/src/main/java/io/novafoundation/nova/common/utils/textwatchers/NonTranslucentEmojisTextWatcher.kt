package io.novafoundation.nova.common.utils.textwatchers

import android.graphics.Color
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan

class NonTranslucentEmojisTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable?) {
        if (editable == null) return

        handleEmojis(editable.toString()) {
            editable.setSpan(ForegroundColorSpan(Color.WHITE), it.first, it.second + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun handleEmojis(string: String, handler: (Pair<Int, Int>) -> Unit) {
        for (i in string.indices) {
            val char = string[i]
            if (Character.isSurrogate(char)) {
                if (i + 1 < string.length) {
                    val nextCharIndex = i + 1
                    val nextChar = string[nextCharIndex]
                    if (Character.isSurrogatePair(char, nextChar)) {
                        handler(i to nextCharIndex)
                    }
                }
            } else if (isEmoji(char)) {
                handler(i to i)
            }
        }
    }

    private fun isEmoji(c: Char): Boolean {
        return c.code in 0x2600..0x27BF ||
            c.code in 0x1F300..0x1F5FF ||
            c.code in 0x1F600..0x1F64F ||
            c.code in 0x1F680..0x1F6FF ||
            c.code in 0x1F900..0x1F9FF
    }
}
