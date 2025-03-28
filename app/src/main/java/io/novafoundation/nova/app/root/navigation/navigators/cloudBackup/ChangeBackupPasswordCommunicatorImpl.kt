package io.novafoundation.nova.app.root.navigation.navigators.cloudBackup

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class ChangeBackupPasswordCommunicatorImpl(private val router: AccountRouter, navigationHoldersRegistry: NavigationHoldersRegistry) :
    NavStackInterScreenCommunicator<ChangeBackupPasswordRequester.EmptyRequest, ChangeBackupPasswordResponder.Success>(navigationHoldersRegistry),
    ChangeBackupPasswordCommunicator {

    override fun openRequest(request: ChangeBackupPasswordRequester.EmptyRequest) {
        super.openRequest(request)

        router.openChangeBackupPasswordFlow()
    }
}
