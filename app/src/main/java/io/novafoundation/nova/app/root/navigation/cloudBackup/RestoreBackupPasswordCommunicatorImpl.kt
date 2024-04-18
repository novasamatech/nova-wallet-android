package io.novafoundation.nova.app.root.navigation.cloudBackup

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class RestoreBackupPasswordCommunicatorImpl(private val router: AccountRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<RestoreBackupPasswordRequester.EmptyRequest, RestoreBackupPasswordResponder.Success>(navigationHolder),
    RestoreBackupPasswordCommunicator {

    override fun openRequest(request: RestoreBackupPasswordRequester.EmptyRequest) {
        super.openRequest(request)

        router.openRestoreBackupPassword()
    }
}
