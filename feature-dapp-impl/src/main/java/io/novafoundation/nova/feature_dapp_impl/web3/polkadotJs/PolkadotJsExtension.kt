package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.mapRawPayloadToSignerPayloadJSON
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.mapRawPayloadToSignerPayloadRaw
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import kotlinx.coroutines.CoroutineScope

class PolkadotJsExtensionFactory(
    private val web3Responder: Web3Responder,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val web3Session: Web3Session,
    private val gson: Gson,
) {

    fun create(scope: CoroutineScope): PolkadotJsExtension {
        return PolkadotJsExtension(
            webViewWeb3JavaScriptInterface = webViewWeb3JavaScriptInterface,
            scope = scope,
            gson = gson,
            web3Responder = web3Responder,
            session = web3Session
        )
    }
}

class PolkadotJsExtension(
    private val gson: Gson,
    private val web3Responder: Web3Responder,
    val session: Web3Session,
    webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    scope: CoroutineScope,
) : WebViewWeb3Extension<PolkadotJsExtensionRequest<*>>(scope, webViewWeb3JavaScriptInterface) {

    override suspend fun messageToRequest(message: String): PolkadotJsExtensionRequest<*>? {
        Log.d(LOG_TAG, message)

        val parsedMessage = gson.parseArbitraryObject(message)!!

        val url = parsedMessage["url"] as? String ?: return null
        val requestId = parsedMessage["id"] as? String ?: return null

        return when (parsedMessage["msgType"]) {
            PolkadotJsExtensionRequest.Identifier.AUTHORIZE_TAB.id ->
                PolkadotJsExtensionRequest.Single.AuthorizeTab(web3Responder, url)

            PolkadotJsExtensionRequest.Identifier.LIST_ACCOUNTS.id ->
                PolkadotJsExtensionRequest.Single.ListAccounts(web3Responder, url, gson)

            PolkadotJsExtensionRequest.Identifier.SIGN_EXTRINSIC.id -> {
                val maybePayload = mapRawPayloadToSignerPayloadJSON(parsedMessage["request"], gson)

                maybePayload?.let {
                    PolkadotJsExtensionRequest.Single.Sign.Extrinsic(web3Responder, url, requestId, maybePayload, gson)
                }
            }

            PolkadotJsExtensionRequest.Identifier.SIGN_BYTES.id -> {
                val maybePayload = mapRawPayloadToSignerPayloadRaw(parsedMessage["request"], gson)

                maybePayload?.let {
                    PolkadotJsExtensionRequest.Single.Sign.Bytes(web3Responder, url, requestId, maybePayload, gson)
                }
            }

            PolkadotJsExtensionRequest.Identifier.SUBSCRIBE_ACCOUNTS.id ->
                PolkadotJsExtensionRequest.Subscription.SubscribeAccounts(scope = this, requestId, web3Responder, url, gson)

            PolkadotJsExtensionRequest.Identifier.LIST_METADATA.id ->
                PolkadotJsExtensionRequest.Single.ListMetadata(web3Responder, url, gson)

            PolkadotJsExtensionRequest.Identifier.PROVIDE_METADATA.id ->
                PolkadotJsExtensionRequest.Single.ProvideMetadata(web3Responder, url)

            else -> null
        }
    }
}
