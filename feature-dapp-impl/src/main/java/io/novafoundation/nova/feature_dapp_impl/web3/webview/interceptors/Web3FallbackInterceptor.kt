package io.novafoundation.nova.feature_dapp_impl.web3.webview.interceptors

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.ToastMessageManager
import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import io.novafoundation.nova.feature_dapp_impl.R

class Web3FallbackInterceptor(
    private val toastMessageManager: ToastMessageManager,
    private val contextManager: ContextManager
) : WebViewRequestInterceptor {

    override fun intercept(request: WebResourceRequest): Boolean {
        val url = request.url

        if (url.scheme != "http" && url.scheme != "https") {
            startIntent(url)
            return true
        }

        return false
    }

    private fun startIntent(url: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url)
            contextManager.getActivity()?.startActivity(intent)
        } catch (e: Exception) {
            val toastText = contextManager.getActivity()?.getString(R.string.common_no_app_to_handle_intent)
            toastText?.let { toastMessageManager.showToast(it) }
        }
    }
}
