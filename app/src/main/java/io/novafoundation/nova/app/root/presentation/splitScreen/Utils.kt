package io.novafoundation.nova.app.root.presentation.splitScreen

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import io.novafoundation.nova.common.utils.applyNavigationBarInsets


fun Insets.withoutBottom(bottomInset: Int): Insets {
    return Insets.of(
        left,
        top,
        right,
        (bottom - bottomInset).coerceAtLeast(0)
    )
}

fun Insets.removeBottom(removeBottom: Boolean): Insets {
    return Insets.of(
        left,
        top,
        right,
        if (removeBottom) 0 else bottom
    )
}
