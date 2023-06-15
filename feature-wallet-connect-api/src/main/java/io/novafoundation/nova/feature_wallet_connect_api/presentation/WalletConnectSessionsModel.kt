package io.novafoundation.nova.feature_wallet_connect_api.presentation

import io.novafoundation.nova.common.utils.formatting.format

class WalletConnectSessionsModel(val connections: String?)

fun mapNumberOfActiveSessionsToUi(activeSessions: Int): WalletConnectSessionsModel {
    return if (activeSessions > 0) {
        WalletConnectSessionsModel(activeSessions.format())
    } else {
        WalletConnectSessionsModel(null)
    }
}
