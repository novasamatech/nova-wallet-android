package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import io.novafoundation.nova.feature_dapp_impl.web3.Web3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.Web3JavascriptResponder

sealed class PolkadotJsExtensionRequest<R>(
    private val web3JavascriptResponder: Web3JavascriptResponder,
    private val identifier: Identifier
) : Web3Extension.Request<R> {

    enum class Identifier(val id: String) {
        AUTHORIZE_TAB("pub(authorize.tab)")
    }

    abstract fun serializeResponse(response: R): String

    override fun reject(error: Throwable) {
        web3JavascriptResponder.respondError(identifier.id, error)
    }

    override fun accept(response: R) {
        web3JavascriptResponder.respondResult(identifier.id, serializeResponse(response))
    }

    class AuthorizeTab(
        web3JavascriptResponder: Web3JavascriptResponder,
        val url: String
    ) : PolkadotJsExtensionRequest<AuthorizeTab.Response>(web3JavascriptResponder, Identifier.AUTHORIZE_TAB) {

        class Response(val authorized: Boolean)

        override fun serializeResponse(response: Response): String {
            return response.authorized.toString()
        }
    }
}
