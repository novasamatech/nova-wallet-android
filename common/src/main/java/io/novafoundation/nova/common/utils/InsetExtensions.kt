package io.novafoundation.nova.common.utils

import android.graphics.Insets
import android.os.Build
import android.view.WindowInsets
import androidx.annotation.RequiresApi

@Suppress("DEPRECATION")
fun WindowInsets.getTopSystemBarInset(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getSystemBarInsets().top
    } else {
        systemWindowInsetTop
    }
}

@Suppress("DEPRECATION")
fun WindowInsets.getBottomSystemBarInset(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getSystemBarInsets().bottom
    } else {
        systemWindowInsetTop
    }
}

@RequiresApi(Build.VERSION_CODES.R)
fun WindowInsets.getSystemBarInsets(): Insets {
    return getInsets(WindowInsets.Type.systemBars())
}
