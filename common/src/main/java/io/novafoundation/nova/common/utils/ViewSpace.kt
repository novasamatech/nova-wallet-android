package io.novafoundation.nova.common.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup

class ViewSpace(
    val start: Int? = null,
    val top: Int? = null,
    val end: Int? = null,
    val bottom: Int? = null
)

operator fun ViewSpace.times(value: Float): ViewSpace {
    return ViewSpace(
        start?.times(value)?.toInt(),
        top?.times(value)?.toInt(),
        end?.times(value)?.toInt(),
        bottom?.times(value)?.toInt()
    )
}

fun ViewSpace.dp(context: Context): ViewSpace {
    return ViewSpace(
        start?.dp(context),
        top?.dp(context),
        end?.dp(context),
        bottom?.dp(context)
    )
}

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
            space.bottom ?: it.bottomMargin
        )
    }
}
