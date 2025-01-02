package io.novafoundation.nova.app.root.navigation.navigators.cloudBackup

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class RestoreBackupPasswordCommunicatorImpl(private val router: AccountRouter, navigationHoldersRegistry: NavigationHoldersRegistry) :
    NavStackInterScreenCommunicator<RestoreBackupPasswordRequester.EmptyRequest, RestoreBackupPasswordResponder.Success>(navigationHoldersRegistry),
    RestoreBackupPasswordCommunicator {

    override fun openRequest(request: RestoreBackupPasswordRequester.EmptyRequest) {
        super.openRequest(request)

        router.openRestoreBackupPassword()
    }
}
