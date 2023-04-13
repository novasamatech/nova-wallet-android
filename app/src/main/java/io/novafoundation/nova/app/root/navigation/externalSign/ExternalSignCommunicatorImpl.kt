package io.novafoundation.nova.app.root.navigation.externalSign

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic.ExternalSignFragment

class ExternalSignCommunicatorImpl(navigationHolder: NavigationHolder) :
    BaseInterScreenCommunicator<ExternalSignPayload, ExternalSignCommunicator.Response>(navigationHolder),
    ExternalSignCommunicator {

    override fun openRequest(request: ExternalSignPayload) {
        navController.navigate(R.id.action_open_externalSignGraph, ExternalSignFragment.getBundle(request))
    }
}
