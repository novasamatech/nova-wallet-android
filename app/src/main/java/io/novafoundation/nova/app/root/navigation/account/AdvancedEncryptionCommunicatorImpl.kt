package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator.Response
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionFragment
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionPayload

class AdvancedEncryptionCommunicatorImpl(
    navigationHolder: NavigationHolder
) : NavStackInterScreenCommunicator<AdvancedEncryptionPayload, Response>(navigationHolder), AdvancedEncryptionCommunicator {

    override fun openRequest(request: AdvancedEncryptionPayload) {
        super.openRequest(request)
        navController.navigate(R.id.action_open_advancedEncryptionFragment, AdvancedEncryptionFragment.getBundle(request))
    }
}
