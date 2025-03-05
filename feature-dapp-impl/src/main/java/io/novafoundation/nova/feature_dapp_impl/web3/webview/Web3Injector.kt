package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.webkit.WebView
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionsStore

interface Web3Injector {

    fun initialInject(into: WebView)

    fun injectForPage(into: WebView, extensionStore: ExtensionsStore)
}

class CompoundWeb3Injector(val injectors: List<Web3Injector>) : Web3Injector {

    override fun initialInject(into: WebView) {
        injectors.forEach { it.initialInject(into) }
    }

    override fun injectForPage(into: WebView, extensionStore: ExtensionsStore) {
        injectors.forEach { it.injectForPage(into, extensionStore) }
    }
}
