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
