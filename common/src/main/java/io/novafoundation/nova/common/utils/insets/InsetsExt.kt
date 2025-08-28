package io.novafoundation.nova.common.utils.insets

import android.view.View
import dev.chrisbanes.insetter.applyInsetter

fun View.applyBarMargin() = applyInsetter {
    type(statusBars = true) {
        margin()
    }
}

fun View.applyStatusBarInsets(consume: Boolean = true) = applyInsetter {
    type(statusBars = true) {
        padding()
    }

    consume(consume)
}

fun View.applyNavigationBarInsets(consume: Boolean = true, imeInsets: ImeInsetsState = ImeInsetsState.DISABLE) = applyInsetter {
    type(navigationBars = true, ime = imeInsets.enabled) {
        padding(bottom = true)
    }

    consume(consume)
}

fun View.applySystemBarInsets(consume: Boolean = true, imeInsets: ImeInsetsState = ImeInsetsState.DISABLE) = applyInsetter {
    type(statusBars = true, navigationBars = true, ime = imeInsets.enabled) {
        padding(top = true, bottom = true)
    }

    consume(consume)
}
