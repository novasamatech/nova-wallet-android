package io.novafoundation.nova.common.view.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext

class BaseAlertDialogBuilder(context: Context) : AlertDialog.Builder(context), CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val backgroundAccessObserver: BackgroundAccessObserver

    init {
        backgroundAccessObserver = FeatureUtils.getCommonApi(context).backgroundAccessObserver()
    }

    override fun create(): AlertDialog {
        val dialog = super.create()
        backgroundAccessObserver.requestAccessFlow
            .onEach {
                if (it == BackgroundAccessObserver.State.REQUEST_ACCESS) {
                    dialog.dismiss()
                }
            }
            .launchIn(this)
        dialog.dismiss()
        dialog.setOnDismissListener { job.cancel() }
        return dialog
    }
}
