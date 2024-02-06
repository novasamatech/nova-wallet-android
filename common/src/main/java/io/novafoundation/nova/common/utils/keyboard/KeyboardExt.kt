package io.novafoundation.nova.common.utils.keyboard

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import io.novafoundation.nova.common.utils.onDestroy

fun View.hideSoftKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.hideSoftKeyboard() {
    currentFocus?.hideSoftKeyboard()
}

fun View.showSoftKeyboard() {
    requestFocus()

    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Make sure that the insets are not consumed by the layer above for this method to work correctly
 */
fun Lifecycle.setKeyboardVisibilityListener(view: View, callback: KeyboardVisibilityCallback?) {
    if (callback == null) {
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
        return
    }

    onDestroy {
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
    }

    var wasShownBefore: Boolean = view.isKeyboardVisible()

    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val isShown = isImeInsetsVisible(insets)
        if (isShown != wasShownBefore) {
            wasShownBefore = isShown
            callback.onKeyboardEvent(isShown)
        }
        insets
    }

    ViewCompat.requestApplyInsets(view)
}

fun Fragment.isKeyboardVisible(): Boolean {
    return requireView().isKeyboardVisible()
}

fun View.isKeyboardVisible(): Boolean {
    val rootInsets = getRootInsets() ?: return false
    return isImeInsetsVisible(rootInsets)
}

private fun View.getRootInsets(): WindowInsetsCompat? {
    return ViewCompat.getRootWindowInsets(this)
}

private fun isImeInsetsVisible(insets: WindowInsetsCompat): Boolean {
    return insets.isVisible(WindowInsetsCompat.Type.ime())
}
