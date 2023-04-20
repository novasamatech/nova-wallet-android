package io.novafoundation.nova.app.root.navigation.externalSign

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.ExternalSignFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class ExternalSignCommunicatorImpl(
    navigationHolder: NavigationHolder,
    private val automaticInteractionGate: AutomaticInteractionGate,
) : CoroutineScope by CoroutineScope(Dispatchers.Main),
    BaseInterScreenCommunicator<ExternalSignPayload, ExternalSignCommunicator.Response>(navigationHolder),
    ExternalSignCommunicator {

    private val _responseFlow = MutableStateFlow<ExternalSignCommunicator.Response?>(null)

    override val latestResponse: ExternalSignCommunicator.Response?
        get() = _responseFlow.value

    override val lastState: ExternalSignCommunicator.Response?
        get() = latestResponse

    override val responseFlow = _responseFlow.filterNotNull()
    override fun respond(response: ExternalSignCommunicator.Response) {
        _responseFlow.value = response
    }

    override fun openRequest(request: ExternalSignPayload) {
        _responseFlow.value = null

        launch {
            automaticInteractionGate.awaitInteractionAllowed()

            navController.navigate(R.id.action_open_externalSignGraph, ExternalSignFragment.getBundle(request))
        }
    }
}
