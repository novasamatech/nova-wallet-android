package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.webkit.WebView
import androidx.annotation.RawRes
import io.novafoundation.nova.common.resources.ResourceManager

private const val JAVASCRIPT_INTERFACE_PREFIX = "Nova"

class WebViewScriptInjector(
    private val resourceManager: ResourceManager
) {

    private enum class InjectionPosition {
        START, END
    }

    private val scriptCache: MutableMap<Int, String> = mutableMapOf()

    fun injectJsInterface(
        into: WebView,
        jsInterface: WebViewWeb3JavaScriptInterface,
        interfaceName: String
    ) {
        val fullName = "${JAVASCRIPT_INTERFACE_PREFIX}_$interfaceName"

        into.addJavascriptInterface(jsInterface, fullName)
    }

    fun injectScript(
        scriptContent: String,
        into: WebView,
        scriptId: String = scriptContent.hashCode().toString(),
    ) {
        addScriptToDomIfNotExists(scriptContent, scriptId, into)
    }

    fun injectScript(
        @RawRes scriptRes: Int,
        into: WebView,
        scriptId: String = scriptRes.toString()
    ) {
        val script = loadScript(scriptRes)

        addScriptToDomIfNotExists(script, scriptId, into)
    }

    private fun loadScript(@RawRes scriptRes: Int) = scriptCache.getOrPut(scriptRes) {
        resourceManager.loadRawString(scriptRes)
    }

    private fun addScriptToDomIfNotExists(
        js: String,
        scriptId: String,
        into: WebView,
    ) {
        val wrappedScript = """
            (function() {
               $js
            })();
        """.trimIndent()

        into.evaluateJavascript(wrappedScript, null)
    }

    private val InjectionPosition.addMethodName
        get() = when (this) {
            InjectionPosition.START -> "prepend"
            InjectionPosition.END -> "appendChild"
        }
}
