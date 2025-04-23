package io.novafoundation.nova.feature_dapp_impl.web3.webview.interceptors

import android.net.Uri
import android.webkit.WebResourceRequest
import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.feature_wallet_connect_api.presentation.utils.WalletConnectUtils

class WalletConnectPairingInterceptor(
    private val walletConnectService: WalletConnectService
) : WebViewRequestInterceptor {

    override fun intercept(request: WebResourceRequest): Boolean {
        if (WalletConnectUtils.isWalletConnectPairingLink(request.url)) {
            pairWithWalletConnect(request.url)
            return true
        }

        return false
    }

    private fun pairWithWalletConnect(url: Uri) {
        walletConnectService.pair(url.toString())
    }
}
