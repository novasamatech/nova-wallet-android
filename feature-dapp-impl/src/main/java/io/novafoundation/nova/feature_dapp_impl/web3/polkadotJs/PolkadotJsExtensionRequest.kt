package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import com.google.gson.Gson
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedAccount
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerResult

sealed class PolkadotJsExtensionRequest<R>(
    private val web3Responder: Web3Responder,
    val url: String,
    private val identifier: Identifier
) : Web3Extension.Request<R> {

    enum class Identifier(val id: String) {
        AUTHORIZE_TAB("pub(authorize.tab)"),
        ACCOUNT_LIST("pub(accounts.list)"),
        SIGN_EXTRINSIC("pub(extrinsic.sign)")
    }

    abstract fun serializeResponse(response: R): String

    override fun reject(error: Throwable) {
        web3Responder.respondError(identifier.id, error)
    }

    override fun accept(response: R) {
        web3Responder.respondResult(identifier.id, serializeResponse(response))
    }

    class AuthorizeTab(
        web3Responder: Web3Responder,
        url: String
    ) : PolkadotJsExtensionRequest<AuthorizeTab.Response>(web3Responder, url, Identifier.AUTHORIZE_TAB) {

        class Response(val authorized: Boolean)

        override fun serializeResponse(response: Response): String {
            return response.authorized.toString()
        }
    }

    class AccountList(
        web3Responder: Web3Responder,
        url: String,
        private val gson: Gson,
    ) : PolkadotJsExtensionRequest<AccountList.Response>(web3Responder, url, Identifier.ACCOUNT_LIST) {

        class Response(val accounts: List<InjectedAccount>)

        override fun serializeResponse(response: Response): String {
            return gson.toJson(response.accounts)
        }
    }

    class SignExtrinsic(
        web3Responder: Web3Responder,
        url: String,
        val requestId: String,
        val signerPayload: SignerPayloadJSON,
        private val gson: Gson,
    ) : PolkadotJsExtensionRequest<SignerResult>(web3Responder, url, Identifier.SIGN_EXTRINSIC) {

        override fun serializeResponse(response: SignerResult): String {
            return gson.toJson(response)
        }
    }
}
