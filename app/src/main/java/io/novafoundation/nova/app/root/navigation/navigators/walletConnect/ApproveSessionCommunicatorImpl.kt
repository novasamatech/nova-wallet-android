package io.novafoundation.nova.app.root.navigation.navigators.walletConnect

import com.walletconnect.web3.wallet.client.Wallet
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.FlowInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.navigationBuilder
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.ApproveSessionCommunicator
import kotlinx.coroutines.launch

class ApproveSessionCommunicatorImpl(
    private val navigationHoldersRegistry: NavigationHoldersRegistry,
    private val automaticInteractionGate: AutomaticInteractionGate,
) : FlowInterScreenCommunicator<Wallet.Model.SessionProposal, Unit>(),
    ApproveSessionCommunicator {

    override fun dispatchRequest(request: Wallet.Model.SessionProposal) {
        launch {
            automaticInteractionGate.awaitInteractionAllowed()

            navigationHoldersRegistry.navigationBuilder().action(R.id.action_open_approve_wallet_connect_session)
                .navigateInFirstAttachedContext()
        }
    }
}
