package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.mapRawPayloadToSignerPayloadJSON
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.mapRawPayloadToSignerPayloadRaw
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3Transport
import kotlinx.coroutines.CoroutineScope

class PolkadotJsTransportFactory(
    private val web3Responder: Web3Responder,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val gson: Gson,
) {

    fun create(scope: CoroutineScope): PolkadotJsTransport {
        return PolkadotJsTransport(
            webViewWeb3JavaScriptInterface = webViewWeb3JavaScriptInterface,
            scope = scope,
            gson = gson,
            web3Responder = web3Responder,
        )
    }
}

class PolkadotJsTransport(
    private val gson: Gson,
    private val web3Responder: Web3Responder,
    webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    scope: CoroutineScope,
) : WebViewWeb3Transport<PolkadotJsTransportRequest<*>>(scope, webViewWeb3JavaScriptInterface) {

    override suspend fun messageToRequest(message: String): PolkadotJsTransportRequest<*>? {
        Log.d(LOG_TAG, message)

        val parsedMessage = gson.parseArbitraryObject(message)!!

        val url = parsedMessage["url"] as? String ?: return null
        val requestId = parsedMessage["id"] as? String ?: return null

        return when (parsedMessage["msgType"]) {
            PolkadotJsTransportRequest.Identifier.AUTHORIZE_TAB.id ->
                PolkadotJsTransportRequest.Single.AuthorizeTab(web3Responder, url, requestId)

            PolkadotJsTransportRequest.Identifier.LIST_ACCOUNTS.id ->
                PolkadotJsTransportRequest.Single.ListAccounts(web3Responder, url, gson, requestId)

            PolkadotJsTransportRequest.Identifier.SIGN_EXTRINSIC.id -> {
                val maybePayload = mapRawPayloadToSignerPayloadJSON(parsedMessage["request"], gson)

                maybePayload?.let {
                    PolkadotJsTransportRequest.Single.Sign.Extrinsic(web3Responder, url, requestId, maybePayload, gson)
                }
            }

            PolkadotJsTransportRequest.Identifier.SIGN_BYTES.id -> {
                val maybePayload = mapRawPayloadToSignerPayloadRaw(parsedMessage["request"], gson)

                maybePayload?.let {
                    PolkadotJsTransportRequest.Single.Sign.Bytes(web3Responder, url, requestId, maybePayload, gson)
                }
            }

            PolkadotJsTransportRequest.Identifier.SUBSCRIBE_ACCOUNTS.id ->
                PolkadotJsTransportRequest.Subscription.SubscribeAccounts(scope = this, requestId, web3Responder, url, gson)

            PolkadotJsTransportRequest.Identifier.LIST_METADATA.id ->
                PolkadotJsTransportRequest.Single.ListMetadata(web3Responder, url, gson, requestId)

            PolkadotJsTransportRequest.Identifier.PROVIDE_METADATA.id ->
                PolkadotJsTransportRequest.Single.ProvideMetadata(web3Responder, url, requestId)

            else -> null
        }
    }
}
