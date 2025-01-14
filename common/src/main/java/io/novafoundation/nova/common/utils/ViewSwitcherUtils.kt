package io.novafoundation.nova.common.utils

import android.widget.TextSwitcher
import android.widget.TextView

fun TextSwitcher.setText(text: String, colorRes: Int) {
    nextTextView.setTextColorRes(colorRes)
    setText(text)
}

fun TextSwitcher.setCurrentText(text: String, colorRes: Int) {
    currentTextView.setTextColorRes(colorRes)
    setCurrentText(text)
}

val TextSwitcher.currentTextView: TextView
    get() = currentView as TextView

val TextSwitcher.nextTextView: TextView
    get() = nextView as TextView
