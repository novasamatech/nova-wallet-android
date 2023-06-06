package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests

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
    private val sessionRequest: Wallet.Model.SessionRequest
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
        }
    }
}

abstract class SignWalletConnectRequest(
    sessionRequest: Wallet.Model.SessionRequest
) : BaseWalletConnectRequest(sessionRequest) {

    override suspend fun sentResponse(response: ExternalSignCommunicator.Response.Sent): Wallet.Params.SessionRequestResponse {
        error("Expected Signed response, got: Sent")
    }
}

abstract class SendTxWalletConnectRequest(
    sessionRequest: Wallet.Model.SessionRequest
) : BaseWalletConnectRequest(sessionRequest) {

    override suspend fun signedResponse(response: ExternalSignCommunicator.Response.Signed): Wallet.Params.SessionRequestResponse {
        error("Expected Sent response, got: Signed")
    }
}
