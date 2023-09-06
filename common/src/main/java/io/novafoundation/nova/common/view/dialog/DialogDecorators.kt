package io.novafoundation.nova.common.view.dialog

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.themed

typealias DialogClickHandler = () -> Unit

typealias DialogDecorator = AlertDialog.Builder.() -> Unit

inline fun dialog(
    context: Context,
    @StyleRes customStyle: Int? = null,
    decorator: DialogDecorator
) {
    val styleOrDefault = customStyle ?: R.style.WhiteOverlay
    val builder = BaseAlertDialogBuilder(ContextThemeWrapper(context, styleOrDefault))
        .setCancelable(false)

    builder.decorator()

    builder.show()
}

fun infoDialog(
    context: Context,
    decorator: DialogDecorator
) {
    dialog(context) {
        setPositiveButton(R.string.common_ok, null)

        decorator()
    }
}

fun warningDialog(
    context: Context,
    onConfirm: DialogClickHandler,
    @StringRes confirmTextRes: Int = R.string.common_continue,
    @StringRes cancelTextRes: Int = R.string.common_cancel,
    onCancel: DialogClickHandler? = null,
    decorator: DialogDecorator? = null
) {
    dialog(context.themed(R.style.AccentNegativeAlertDialogTheme_Reversed)) {
        setPositiveButton(confirmTextRes) { _, _ -> onConfirm() }
        setNegativeButton(cancelTextRes) { _, _ -> onCancel?.invoke() }

        decorator?.invoke(this)
    }
}

fun errorDialog(
    context: Context,
    onConfirm: DialogClickHandler? = null,
    @StringRes confirmTextRes: Int = R.string.common_ok,
    decorator: DialogDecorator? = null
) {
    dialog(context) {
        setTitle(R.string.common_error_general_title)
        setPositiveButton(confirmTextRes) { _, _ -> onConfirm?.invoke() }

        decorator?.invoke(this)
    }
}

fun retryDialog(
    context: Context,
    onRetry: DialogClickHandler? = null,
    onCancel: DialogClickHandler? = null,
    decorator: DialogDecorator? = null
) {
    dialog(context) {
        setTitle(R.string.common_error_general_title)
        setPositiveButton(R.string.common_retry) { _, _ -> onRetry?.invoke() }
        setNegativeButton(R.string.common_ok) { _, _ -> onCancel?.invoke() }

        decorator?.invoke(this)
    }
}
