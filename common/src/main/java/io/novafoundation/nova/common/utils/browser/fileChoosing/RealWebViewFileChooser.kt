package io.novafoundation.nova.common.utils.browser.fileChoosing

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.systemCall.WebViewFilePickerSystemCallFactory
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor

class WebViewFileChooserFactory(
    private val systemCallExecutor: SystemCallExecutor,
    private val webViewFilePickerSystemCallFactory: WebViewFilePickerSystemCallFactory
) {
    fun create(fragment: Fragment): WebViewFileChooser {
        return RealWebViewFileChooser(systemCallExecutor, webViewFilePickerSystemCallFactory, fragment)
    }
}

class RealWebViewFileChooser(
    private val systemCallExecutor: SystemCallExecutor,
    private val webViewFilePickerSystemCallFactory: WebViewFilePickerSystemCallFactory,
    private val fragment: Fragment
) : WebViewFileChooser {

    override fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?): Boolean {
        val systemCall = webViewFilePickerSystemCallFactory.create(fileChooserParams)

        val isHandled = systemCallExecutor.executeSystemCallNotBlocking(systemCall) {
            it.onSuccess { uri ->
                filePathCallback?.onReceiveValue(arrayOf(uri))
            }.onFailure {
                filePathCallback?.onReceiveValue(null)
            }
        }

        if (!isHandled) {
            Toast.makeText(fragment.context, R.string.common_no_app_to_handle_import_intent, Toast.LENGTH_LONG)
                .show()
        }

        return isHandled
    }
}
