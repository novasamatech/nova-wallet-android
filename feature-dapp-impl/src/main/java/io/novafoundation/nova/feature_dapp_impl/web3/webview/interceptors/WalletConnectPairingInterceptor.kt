package io.novafoundation.nova.feature_dapp_impl.web3.webview.interceptors

import android.net.Uri
import android.webkit.WebResourceRequest
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import io.novafoundation.nova.feature_wallet_connect_api.presentation.utils.WalletConnectUtils

class WalletConnectPairingInterceptor(
    private val contextManager: ContextManager,
    private val activityIntentProvider: ActivityIntentProvider
) : WebViewRequestInterceptor {

    override fun intercept(request: WebResourceRequest): Boolean {
        if (WalletConnectUtils.isWalletConnectPairingLink(request.url)) {
            runIntent(request.url)
            return true
        }

        return false
    }

    private fun runIntent(url: Uri) {
        val intent = activityIntentProvider.getIntent().setData(url)
        contextManager.getActivity()?.startActivity(intent)
    }
}
