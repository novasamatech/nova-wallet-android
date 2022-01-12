package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedAccount
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

sealed class PolkadotJsExtensionRequest<R>(
    protected val web3Responder: Web3Responder,
    protected val identifier: Identifier,
    val url: String,
) : Web3Extension.Request<R> {

    override fun reject(error: Throwable) {
        web3Responder.respondError(identifier.id, error)
    }

    enum class Identifier(val id: String) {
        AUTHORIZE_TAB("pub(authorize.tab)"),
        LIST_ACCOUNTS("pub(accounts.list)"),
        SIGN_EXTRINSIC("pub(extrinsic.sign)"),
        SUBSCRIBE_ACCOUNTS("pub(accounts.subscribe)")
    }

    sealed class Single<R>(
        web3Responder: Web3Responder,
        url: String,
        identifier: Identifier
    ) : PolkadotJsExtensionRequest<R>(web3Responder, identifier, url) {

        abstract fun serializeResponse(response: R): String

        override fun accept(response: R) {
            web3Responder.respondResult(identifier.id, serializeResponse(response))
        }

        class AuthorizeTab(
            web3Responder: Web3Responder,
            url: String
        ) : Single<AuthorizeTab.Response>(web3Responder, url, Identifier.AUTHORIZE_TAB) {

            class Response(val authorized: Boolean)

            override fun serializeResponse(response: Response): String {
                return response.authorized.toString()
            }
        }

        class ListAccounts(
            web3Responder: Web3Responder,
            url: String,
            private val gson: Gson,
        ) : Single<List<InjectedAccount>>(web3Responder, url, Identifier.LIST_ACCOUNTS) {

            override fun serializeResponse(response: List<InjectedAccount>): String {
                return gson.toJson(response)
            }
        }

        class SignExtrinsic(
            web3Responder: Web3Responder,
            url: String,
            val requestId: String,
            val signerPayload: SignerPayloadJSON,
            private val gson: Gson,
        ) : Single<SignerResult>(web3Responder, url, Identifier.SIGN_EXTRINSIC) {

            override fun serializeResponse(response: SignerResult): String {
                return gson.toJson(response)
            }
        }
    }

    sealed class Subscription<R>(
        private val scope: CoroutineScope,
        private val requestId: String,
        web3Responder: Web3Responder,
        url: String,
        identifier: Identifier
    ) : PolkadotJsExtensionRequest<Flow<R>>(web3Responder, identifier, url) {

        abstract fun serializeSubscriptionResponse(response: R): String

        override fun accept(response: Flow<R>) {
            web3Responder.respondResult(identifier.id, "true")

            response
                .map(::serializeSubscriptionResponse)
                .onEach { web3Responder.respondSubscription(requestId, it) }
                .inBackground()
                .launchIn(scope)
        }

        class SubscribeAccounts(
            scope: CoroutineScope,
            requestId: String,
            web3Responder: Web3Responder,
            url: String,
            private val gson: Gson,
        ) : Subscription<List<InjectedAccount>>(scope, requestId, web3Responder, url, Identifier.SUBSCRIBE_ACCOUNTS) {

            override fun serializeSubscriptionResponse(response: List<InjectedAccount>): String {
                return gson.toJson(response)
            }
        }
    }
}
