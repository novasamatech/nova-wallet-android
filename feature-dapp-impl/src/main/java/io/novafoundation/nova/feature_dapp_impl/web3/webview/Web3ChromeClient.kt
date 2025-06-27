package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooser
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAsker
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.webView.BaseWebChromeClient
import kotlinx.coroutines.CoroutineScope

private const val MAX_PROGRESS = 100

class Web3ChromeClient(
    permissionAsker: WebViewPermissionAsker,
    fileChooser: WebViewFileChooser,
    coroutineScope: CoroutineScope,
    private val progressBar: ProgressBar
) : BaseWebChromeClient(permissionAsker, fileChooser, coroutineScope) {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        progressBar.progress = newProgress

        progressBar.setVisible(newProgress < MAX_PROGRESS)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Log.d("Web3ChromeClient", consoleMessage.message())
        return true
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        val newWebView = WebView(view.context)
        newWebView.settings.javaScriptEnabled = true
        newWebView.webChromeClient = this
        newWebView.webViewClient = WebViewClient()

        Toast.makeText(view.context, "New Window Started!", Toast.LENGTH_SHORT).show()

        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()
        return true
    }
}
