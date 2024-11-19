package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import io.novafoundation.nova.common.utils.setVisible

private const val MAX_PROGRESS = 100

class Web3ChromeClient(
    private val fileChooser: WebViewFileChooser,
    private val progressBar: ProgressBar
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        progressBar.progress = newProgress

        progressBar.setVisible(newProgress < MAX_PROGRESS)
    }

    override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
        fileChooser.onShowFileChooser(filePathCallback, fileChooserParams)
        return true
    }
}
