package io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.Wallet.Model.Namespace.Session
import com.reown.walletkit.client.Wallet.Model.SessionProposal
import com.reown.walletkit.client.Wallet.Params.SessionApprove
import com.reown.walletkit.client.WalletKit
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

fun Wallet.Model.SessionRequest.rejected(): Wallet.Params.SessionRequestResponse {
    return failed(WalletConnectError.REJECTED)
}

suspend fun WalletKit.approveSession(approve: SessionApprove): Result<Unit> {
    return suspendCoroutine { continuation ->
        approveSession(
            params = approve,
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { continuation.resume(Result.failure(it.throwable)) }
        )
    }
}

suspend fun WalletKit.rejectSession(reject: Wallet.Params.SessionReject): Result<Unit> {
    return suspendCoroutine { continuation ->
        rejectSession(
            params = reject,
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { continuation.resume(Result.failure(it.throwable)) }
        )
    }
}

suspend fun WalletKit.disconnectSession(sessionTopic: String): Result<Unit> {
    return suspendCoroutine { continuation ->
        disconnectSession(
            params = Wallet.Params.SessionDisconnect(sessionTopic),
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { continuation.resume(Result.failure(it.throwable)) }
        )
    }
}

suspend fun WalletKit.respondSessionRequest(response: Wallet.Params.SessionRequestResponse): Result<Unit> {
    return suspendCoroutine { continuation ->
        respondSessionRequest(
            params = response,
            onSuccess = { continuation.resume(Result.success(Unit)) },
            onError = { continuation.resume(Result.failure(it.throwable)) }
        )
    }
}
