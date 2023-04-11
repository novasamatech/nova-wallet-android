package io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk

import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.Namespace.Session
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionProposal
import com.walletconnect.web3.wallet.client.Wallet.Params.SessionApprove
import com.walletconnect.web3.wallet.client.Web3Wallet
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun SessionProposal.approved(namespaces: Map<String, Session>): SessionApprove {
    return SessionApprove(
        proposerPublicKey = proposerPublicKey,
        namespaces = namespaces,
        relayProtocol = relayProtocol
    )
}

fun SessionProposal.rejected(reason: String): Wallet.Params.SessionReject {
    return Wallet.Params.SessionReject(
        proposerPublicKey = proposerPublicKey,
        reason = reason
    )
}

fun Wallet.Model.SessionRequest.approved(result: String): Wallet.Params.SessionRequestResponse {
    return Wallet.Params.SessionRequestResponse(
        sessionTopic = topic,
        jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
            id = request.id,
            result = result
        )
    )
}

class WalletConnectError(val code: Int, val message: String) {

   companion object {
       val REJECTED = WalletConnectError(5000, "Rejected by user")

       val GENERAL_FAILURE = WalletConnectError(0, "Unknown error")
   }
}

fun Wallet.Model.SessionRequest.failed(error: WalletConnectError): Wallet.Params.SessionRequestResponse {
    return Wallet.Params.SessionRequestResponse(
        sessionTopic = topic,
        jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
            id = request.id,
            code = error.code,
            message = error.message
        )
    )
}

fun Wallet.Model.SessionRequest.rejected(): Wallet.Params.SessionRequestResponse {
    return failed(WalletConnectError.REJECTED)
}

suspend fun Web3Wallet.approveSession(approve: SessionApprove): Result<Unit> {
    return suspendCoroutine { continuation ->
        approveSession(
            params = approve,
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { continuation.resume(Result.failure(it.throwable)) }
        )
    }
}

suspend fun Web3Wallet.rejectSession(reject: Wallet.Params.SessionReject): Result<Unit> {
    return suspendCoroutine { continuation ->
        rejectSession(
            params = reject,
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { continuation.resume(Result.failure(it.throwable)) }
        )
    }
}

suspend fun Web3Wallet.respondSessionRequest(response: Wallet.Params.SessionRequestResponse): Result<Unit> {
    return suspendCoroutine { continuation ->
        respondSessionRequest(
            params = response,
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { continuation.resume(Result.failure(it.throwable)) }
        )
    }
}
