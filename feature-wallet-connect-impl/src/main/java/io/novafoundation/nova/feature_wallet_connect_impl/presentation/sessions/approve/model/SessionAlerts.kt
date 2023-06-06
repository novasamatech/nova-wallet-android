package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.model

class SessionAlerts(
    val missingAccounts: MissingAccounts?,
    val unsupportedChains: UnsupportedChains?
) {

    class MissingAccounts(val alertContent: String)

    class UnsupportedChains(val alertContent: String)
}

fun SessionAlerts.hasBlockingAlerts(): Boolean {
    return missingAccounts != null || unsupportedChains != null
}
