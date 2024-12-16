package io.novafoundation.nova.feature_dapp_core.web3.injector

import android.webkit.WebView

interface Web3ScriptInjector {

    fun initialInject(into: WebView)

    fun injectForPage(into: WebView)
}
