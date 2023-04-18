package io.novafoundation.nova.common.utils

import android.app.Dialog

class SingletonDialogHolder<T : Dialog> {

    private var dialog: T? = null

    fun isDialogAlreadyOpened(): Boolean {
        return dialog != null && dialog?.isShowing == true
    }

    fun getDialog(): T? {
        return dialog
    }

    fun showNewDialogOrSkip(creator: () -> T) {
        if (isDialogAlreadyOpened()) return

        dialog = creator()
        dialog?.setOnDismissListener {
            dialog = null
        }

        dialog?.show()
    }
}
