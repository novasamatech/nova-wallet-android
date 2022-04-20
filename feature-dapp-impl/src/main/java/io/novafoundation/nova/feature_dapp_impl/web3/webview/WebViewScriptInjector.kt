package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.util.Base64
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
        val encoded: String = Base64.encodeToString(js.encodeToByteArray(), Base64.NO_WRAP)
        val method = InjectionPosition.START.addMethodName

        val initializationCode = """
            var parent = document.getElementsByTagName('body').item(0);
            var prevScripts = parent.getElementsByClassName("$scriptId")
            console.log("Injecting $scriptId")
            if (prevScripts.length== 0) {
                var script = document.createElement('script');                 
                script.type = 'text/javascript';
                script.innerHTML = window.atob('$encoded');
                script.className = "$scriptId";
                parent.$method(script);
            }
        """.trimIndent()

        val wrappedScript = """
            if (document !== undefined && document.readyState !== 'loading') {
                $initializationCode
            } else {
                window.addEventListener("DOMContentLoaded", function(event) {
                 $initializationCode
                });
            }
        """.trimIndent()

        into.evaluateJavascript(wrappedScript, null)
    }

    private val InjectionPosition.addMethodName
        get() = when (this) {
            InjectionPosition.START -> "prepend"
            InjectionPosition.END -> "appendChild"
        }
}
