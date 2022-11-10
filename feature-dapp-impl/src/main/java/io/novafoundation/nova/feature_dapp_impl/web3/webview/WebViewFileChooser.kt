package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.novafoundation.nova.feature_dapp_impl.R

class WebViewFileChooser(
    private val fragment: Fragment
) {

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            val uri = data?.data
            if (uri != null) {
                fileChooserCallback?.onReceiveValue(arrayOf(uri))
            } else {
                fileChooserCallback?.onReceiveValue(null)
            }
            fileChooserCallback = null
        }
    }

    fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?): Boolean {
        if (fileChooserCallback != null) {
            fileChooserCallback?.onReceiveValue(null)
        }
        fileChooserCallback = filePathCallback

        val chooserIntent = if (fileChooserParams == null) {
            val selectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            selectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            selectionIntent.type = "*/*"
            Intent.createChooser(selectionIntent, null)
        } else {
            fileChooserParams.createIntent()
        }

        try {
            fragment.startActivityForResult(chooserIntent, REQUEST_CODE)
            return true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(fragment.context, R.string.common_no_app_to_handle_import_intent, Toast.LENGTH_LONG)
                .show()
            return false
        }
    }

    companion object {
        private const val REQUEST_CODE = 881
    }
}
