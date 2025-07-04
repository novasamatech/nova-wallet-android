package io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk

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

class WalletConnectError(val code: Int, override val message: String) : Throwable() {

    companion object {
        val REJECTED = WalletConnectError(5000, "Rejected by user")

        val SEND_HANDLED_BY_SIGNER = WalletConnectError(50001, "Sending extrinsic was handled by Nova Wallet directly")

        val GENERAL_FAILURE = WalletConnectError(0, "Unknown error")

        val NO_SESSION_FOR_TOPIC = WalletConnectError(7001, "No session for topic")

        val UNAUTHORIZED_METHOD = WalletConnectError(3001, "Unauthorized method")

        val CHAIN_MISMATCH = WalletConnectError(1001, "Wrong chain id passed by dApp")

        fun UnknownMethod(method: String) = WalletConnectError(3001, "$method is not supported")
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

fun Wallet.Model.SessionRequest.rejected(reason: WalletConnectError = WalletConnectError.REJECTED): Wallet.Params.SessionRequestResponse {
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

suspend fun Web3Wallet.disconnectSession(sessionTopic: String): Result<Unit> {
    return suspendCoroutine { continuation ->
        disconnectSession(
            params = Wallet.Params.SessionDisconnect(sessionTopic),
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
