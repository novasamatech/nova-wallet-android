package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.webkit.WebView
import android.widget.ProgressBar
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
}
