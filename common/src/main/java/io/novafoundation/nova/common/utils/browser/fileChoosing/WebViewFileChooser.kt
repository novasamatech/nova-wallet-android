package io.novafoundation.nova.common.utils.browser.fileChoosing

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

interface WebViewFileChooser {

    fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?): Boolean
}
