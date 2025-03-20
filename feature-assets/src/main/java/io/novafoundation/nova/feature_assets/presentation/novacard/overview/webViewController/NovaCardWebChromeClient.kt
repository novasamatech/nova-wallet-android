package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooser
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAsker
import kotlinx.coroutines.CoroutineScope

class NovaCardWebChromeClientFactory(
    private val permissionsAsker: WebViewPermissionAsker,
    private val webViewFileChooser: WebViewFileChooser
) {
    fun create(coroutineScope: CoroutineScope) = NovaCardWebChromeClient(
        permissionsAsker = permissionsAsker,
        webViewFileChooser = webViewFileChooser,
        coroutineScope = coroutineScope
    )
}

class NovaCardWebChromeClient(
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
