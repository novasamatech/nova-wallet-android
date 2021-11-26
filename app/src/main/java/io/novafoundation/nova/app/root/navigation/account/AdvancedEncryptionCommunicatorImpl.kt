package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator.Request
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator.Response

class AdvancedEncryptionCommunicatorImpl(
    navigationHolder: NavigationHolder
) : BaseInterScreenCommunicator<Request, Response>(navigationHolder), AdvancedEncryptionCommunicator {

    override fun openRequest(request: Request) {
        TODO("Not yet implemented")
    }
}
