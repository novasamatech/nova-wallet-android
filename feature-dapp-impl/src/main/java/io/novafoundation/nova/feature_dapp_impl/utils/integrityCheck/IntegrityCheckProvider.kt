package io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableSharedFlow

class IntegrityCheckProviderFactory(
    private val integrityCheckSessionFactory: IntegrityCheckSessionFactory
) {

    fun create(webView: WebView): IntegrityCheckProvider {
        return IntegrityCheckProvider(
            integrityCheckSessionFactory,
            webView
        )
    }
}

private const val APP_INTEGRITY_ID_NOT_FOUND_CODE = 400

class IntegrityCheckProvider(
    private val integrityCheckSessionFactory: IntegrityCheckSessionFactory,
    private val webView: WebView
) : IntegrityCheckSession.Callback {

    private var session: IntegrityCheckSession? = null
    private val errorFlow = MutableSharedFlow<String>()

    suspend fun onRequestIntegrityCheck(baseUrl: String) {
        session = integrityCheckSessionFactory.createSession(baseUrl, this)
        session?.startIntegrityCheck()
    }

    suspend fun signatureVerificationError(code: Int, error: String) {
        if (session == null) return

        if (code == APP_INTEGRITY_ID_NOT_FOUND_CODE) {
            session?.restartIntegrityCheck()
        } else {
            errorFlow.emit(error)
        }
    }

    fun onPageUpdated() {
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
}
