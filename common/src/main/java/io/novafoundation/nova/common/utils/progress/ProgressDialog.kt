package io.novafoundation.nova.common.utils.progress

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R

class ProgressDialog(context: Context) : Dialog(context) {

    init {
        setContentView(R.layout.dialog_progress)
        setCancelable(false)
    }

    fun setText(@StringRes textRes: Int) {
        findViewById<TextView>(R.id.progressText).setText(textRes)
    }
}
