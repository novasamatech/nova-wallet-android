package io.novafoundation.nova.feature_wallet_connect_impl

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsPayload

interface WalletConnectRouter : ReturnableRouter {

    fun openSessionDetails(payload: WalletConnectSessionDetailsPayload)

    fun openScanPairingQrCode()

    fun backToSettings()
}
