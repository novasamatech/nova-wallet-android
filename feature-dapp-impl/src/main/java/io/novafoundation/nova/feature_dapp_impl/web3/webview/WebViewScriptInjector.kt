package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.util.Base64
import android.webkit.WebView
import androidx.annotation.RawRes
import io.novafoundation.nova.common.resources.ResourceManager

// should be in tact with javascript_interface_bridge.js
private const val JAVASCRIPT_INTERFACE_NAME = "Nova"

class WebViewScriptInjector(
    private val web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val resourceManager: ResourceManager
) {

    enum class InjectionPosition {
        START, END
    }

    private val scriptCache: MutableMap<Int, String> = mutableMapOf()

    fun injectJsInterface(into: WebView) {
        into.addJavascriptInterface(web3JavaScriptInterface, JAVASCRIPT_INTERFACE_NAME)
    }

    fun injectScript(
        @RawRes scriptRes: Int,
        into: WebView,
        injectionPosition: InjectionPosition = InjectionPosition.START
    ) {
        val script = loadScript(scriptRes)
        val scriptId = scriptRes.toString()

        addScriptToDomIfNotExists(script, injectionPosition, scriptId, into)
    }

    private fun loadScript(@RawRes scriptRes: Int) = scriptCache.getOrPut(scriptRes) {
        resourceManager.loadRawString(scriptRes)
    }

    private fun addScriptToDomIfNotExists(
        js: String,
        injectionPosition: InjectionPosition,
        scriptId: String,
        into: WebView,
    ) {
        val encoded: String = Base64.encodeToString(js.encodeToByteArray(), Base64.NO_WRAP)
        val method = injectionPosition.addMethodName

        val wrappedScript = """
        var parent = document.getElementsByTagName('body').item(0);
        var prevScripts = parent.getElementsByClassName("$scriptId")
        if (prevScripts.length== 0) {
            var script = document.createElement('script');                 
            script.type = 'text/javascript';
            script.innerHTML = window.atob('$encoded');
            script.className = "$scriptId";
            parent.$method(script);
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
