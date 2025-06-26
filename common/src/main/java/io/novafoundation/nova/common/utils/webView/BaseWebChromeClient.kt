package io.novafoundation.nova.common.utils.webView

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooser
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAsker
import kotlinx.coroutines.CoroutineScope

class BaseWebChromeClientFactory(
    private val permissionsAsker: WebViewPermissionAsker,
    private val webViewFileChooser: WebViewFileChooser
) {
    fun create(coroutineScope: CoroutineScope) = BaseWebChromeClient(
        permissionsAsker = permissionsAsker,
        webViewFileChooser = webViewFileChooser,
        coroutineScope = coroutineScope
    )
}

open class BaseWebChromeClient(
    private val permissionsAsker: WebViewPermissionAsker,
    private val webViewFileChooser: WebViewFileChooser,
    private val coroutineScope: CoroutineScope
) : WebChromeClient() {

    override fun onPermissionRequest(request: PermissionRequest) {
        permissionsAsker.requestPermission(coroutineScope, request)
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        webViewFileChooser.onShowFileChooser(filePathCallback, fileChooserParams)

        return true
    }
}
