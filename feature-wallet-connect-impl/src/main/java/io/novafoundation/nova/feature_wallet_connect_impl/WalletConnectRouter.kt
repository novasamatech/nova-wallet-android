package io.novafoundation.nova.feature_wallet_connect_impl

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsPayload
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload

interface WalletConnectRouter : ReturnableRouter {

    fun openSessionDetails(payload: WalletConnectSessionDetailsPayload)

    fun openScanPairingQrCode()

    fun backToSettings()

    fun openWalletConnectSessions(payload: WalletConnectSessionsPayload)
}
