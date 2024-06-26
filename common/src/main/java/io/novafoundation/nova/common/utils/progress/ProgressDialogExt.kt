package io.novafoundation.nova.common.utils.progress

import android.app.Dialog
import io.novafoundation.nova.common.base.BaseFragmentMixin

fun BaseFragmentMixin<*>.observeProgressDialog(progressDialogMixin: ProgressDialogMixin): Dialog {
    val progressDialog = ProgressDialog(providedContext)
    progressDialogMixin.showProgressLiveData.observeEvent {
        when (it) {
            is ProgressState.Show -> {
                progressDialog.setText(it.textRes)
                progressDialog.show()
            }

            is ProgressState.Hide -> progressDialog.dismiss()
        }
    }
    return progressDialog
}
