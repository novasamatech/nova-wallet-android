package io.novafoundation.nova.common.utils

import android.view.View
import android.view.ViewGroup

class ViewSpace(
    val start: Int? = null,
    val top: Int? = null,
    val end: Int? = null,
    val bottom: Int? = null
)

fun View.updatePadding(space: ViewSpace) {
    setPadding(
        space.start ?: paddingStart,
        space.top ?: paddingTop,
        space.end ?: paddingEnd,
        space.bottom ?: paddingBottom
    )
}

fun View.updateMargin(space: ViewSpace) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        it.setMargins(
            space.start ?: it.marginStart,
            space.top ?: it.topMargin,
            space.end ?: it.marginEnd,
            space.bottom ?: it.bottomMargin)
    }
}
