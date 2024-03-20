package io.novafoundation.nova.app.root.presentation.deepLinks.handlers.walletConnect

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
        // Older version of wc send both pair and sign requests through `wc:` deeplink so we additionaly check for `symKey` which is only present in pairing url
        val oldLinkMatch = data.scheme == "wc" && "symKey" in data.toString()

        return newLinkMatch || oldLinkMatch
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()
        walletConnectService.pair(data.toString())
    }
}
