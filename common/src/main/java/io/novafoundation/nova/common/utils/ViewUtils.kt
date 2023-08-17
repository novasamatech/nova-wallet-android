package io.novafoundation.nova.common.utils

import android.view.View

fun makeVisibleViews(vararg views: View) {
    views.forEach { it.visibility = View.VISIBLE }
}

fun makeGoneViews(vararg views: View) {
    views.forEach { it.visibility = View.GONE }
}
