package io.novafoundation.nova.app.root.navigation.navigators.externalSign

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.FlowInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.navigationBuilder
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.ExternalSignFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExternalSignCommunicatorImpl(
    private val navigationHoldersRegistry: NavigationHoldersRegistry,
    private val automaticInteractionGate: AutomaticInteractionGate,
) : CoroutineScope by CoroutineScope(Dispatchers.Main),
    FlowInterScreenCommunicator<ExternalSignPayload, ExternalSignCommunicator.Response>(),
    ExternalSignCommunicator {

    override fun dispatchRequest(request: ExternalSignPayload) {
        launch {
            automaticInteractionGate.awaitInteractionAllowed()

            navigationHoldersRegistry.navigationBuilder(R.id.action_open_externalSignGraph)
                .setArgs(ExternalSignFragment.getBundle(request))
                .navigateInRoot()
        }
    }
}
