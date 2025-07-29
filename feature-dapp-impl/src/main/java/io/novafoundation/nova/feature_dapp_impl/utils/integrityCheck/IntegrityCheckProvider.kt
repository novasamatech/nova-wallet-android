package io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck

import android.webkit.JavascriptInterface
import android.webkit.WebView
import io.novafoundation.nova.common.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

class IntegrityCheckProviderFactory(
    private val integrityCheckSessionFactory: IntegrityCheckSessionFactory
) {

    fun create(webView: WebView, coroutineScope: CoroutineScope): IntegrityCheckProvider {
        return IntegrityCheckProvider(
            integrityCheckSessionFactory,
            webView,
            coroutineScope
        )
    }
}

private const val JAVASCRIPT_INTERFACE_NAME = "IntegrityProvider"
private const val APP_INTEGRITY_ID_NOT_FOUND_CODE = 400

class IntegrityCheckProvider(
    private val integrityCheckSessionFactory: IntegrityCheckSessionFactory,
    private val webView: WebView,
    private val coroutineScope: CoroutineScope
) : IntegrityCheckSession.Callback, CoroutineScope by coroutineScope {

    val errorFlow = MutableSharedFlow<String>()

    private var session: IntegrityCheckSession? = null
    private val providerJsCallback = IntegrityProviderJsCallback()

    init {
        webView.addJavascriptInterface(providerJsCallback, JAVASCRIPT_INTERFACE_NAME)
    }

    fun onRequestIntegrityCheck(baseUrl: String) = launchUnit {
        log("request integrity check. BaseUrl: $baseUrl")
        session = integrityCheckSessionFactory.createSession(baseUrl, this@IntegrityCheckProvider)
        runCatching { session?.startIntegrityCheck() }
            .onFailure { it.message?.let { errorFlow.emit(it) } }
    }

    fun onSignatureVerificationError(code: Int, error: String) = launchUnit {
        log("signature verification error")
        if (session == null) return@launchUnit

        if (code == APP_INTEGRITY_ID_NOT_FOUND_CODE) {
            log("restart integrity check")
            runCatching { session?.restartIntegrityCheck() }
                .onFailure { it.message?.let { errorFlow.emit(it) } }
        } else {
            errorFlow.emit(error)
        }
    }

    fun onPageFinished() {
        session?.let { log("clear integrity check session") }
        session?.removeCallback()
        session = null
    }

    override fun sendVerificationRequest(challenge: String, appIntegrityId: String, signature: String) {
        val jsCode = """
        window.verifySignature({
            challenge: "$challenge",
            appIntegrityId: "$appIntegrityId",
            signature: "$signature",
            platform: "ANDROID"
        });
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }

    private fun log(message: String) = webView.evaluateJavascript("console.log('$message')", null)

    inner class IntegrityProviderJsCallback {
        @JavascriptInterface
        fun requestIntegrityCheck(baseUrl: String) {
            onRequestIntegrityCheck(baseUrl)
        }

        @JavascriptInterface
        fun signatureVerificationError(code: Int, error: String) {
            onSignatureVerificationError(code, error)
        }
    }
}
