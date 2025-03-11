package io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class WalletConnectPairDeeplinkHandler(
    private val walletConnectService: WalletConnectService,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val newLinkMatch = data.scheme == "novawallet" && data.host == "wc"
        val oldLinkMatch = data.scheme == "wc"
        val linkMatch = newLinkMatch || oldLinkMatch

        val isPairing = "symKey" in data.toString()

        return linkMatch && isPairing
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()
        walletConnectService.pair(data.toString())
    }
}
