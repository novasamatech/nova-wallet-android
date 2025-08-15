package io.novafoundation.nova.app.root.presentation.splitScreen

import androidx.core.graphics.Insets

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
