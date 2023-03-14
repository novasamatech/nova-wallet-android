package io.novafoundation.nova.common.utils

import android.content.DialogInterface
import android.view.View

// TODO waiting for multiple receivers feature, probably in Kotlin 1.7
interface DialogExtensions {

    val dialogInterface: DialogInterface

    fun View.setDismissingClickListener(listener: (View) -> Unit) {
        setOnClickListener {
            listener.invoke(it)

            dialogInterface.dismiss()
        }
    }

    fun View.dismissOnClick() {
        setOnClickListener { dialogInterface.dismiss() }
    }
}
