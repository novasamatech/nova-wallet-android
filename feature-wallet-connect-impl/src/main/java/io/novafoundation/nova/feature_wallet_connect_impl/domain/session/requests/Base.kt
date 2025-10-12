package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests

import android.content.Context
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.WalletConnectError
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.failed
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.rejected
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.respondSessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseWalletConnectRequest(
    private val sessionRequest: Wallet.Model.SessionRequest,
    private val context: Context,
) : WalletConnectRequest {

    override val id: String = sessionRequest.request.id.toString()

    abstract suspend fun sentResponse(response: ExternalSignCommunicator.Response.Sent): Wallet.Params.SessionRequestResponse

    abstract suspend fun signedResponse(response: ExternalSignCommunicator.Response.Signed): Wallet.Params.SessionRequestResponse

    override suspend fun respondWith(response: ExternalSignCommunicator.Response): Result<*> = kotlin.runCatching {
        withContext(Dispatchers.Default) {
            val walletConnectResponse = when (response) {
                is ExternalSignCommunicator.Response.Rejected -> sessionRequest.rejected()
                is ExternalSignCommunicator.Response.Sent -> sentResponse(response)
                is ExternalSignCommunicator.Response.Signed -> signedResponse(response)
                is ExternalSignCommunicator.Response.SigningFailed -> sessionRequest.failed(WalletConnectError.GENERAL_FAILURE)
            }

            Web3Wallet.respondSessionRequest(walletConnectResponse).getOrThrow()

            // TODO this code is untested since no dapp currently use redirect param
            // We can't really enable this code without testing since we need to verify a corner-case when wc is used with redirect param inside dapp browser
            // This might potentially break user flow since it might direct user to external browser instead of staying in our dapp browser

//            val redirect = sessionRequest.peerMetaData?.redirect
//            if (!redirect.isNullOrEmpty()) {
//                context.startActivity(Intent(Intent.ACTION_VIEW, redirect.toUri()))
//            }
        }
    }
}

abstract class SignWalletConnectRequest(
    sessionRequest: Wallet.Model.SessionRequest,
    context: Context
) : BaseWalletConnectRequest(sessionRequest, context) {

    override suspend fun sentResponse(response: ExternalSignCommunicator.Response.Sent): Wallet.Params.SessionRequestResponse {
        error("Expected Signed response, got: Sent")
    }
}

abstract class SendTxWalletConnectRequest(
    sessionRequest: Wallet.Model.SessionRequest,
    context: Context
) : BaseWalletConnectRequest(sessionRequest, context) {

    override suspend fun signedResponse(response: ExternalSignCommunicator.Response.Signed): Wallet.Params.SessionRequestResponse {
        error("Expected Sent response, got: Signed")
    }
}
