package io.novafoundation.nova.app.root.navigation.cloudBackup

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordResponder

class SyncWalletsBackupPasswordCommunicatorImpl(private val router: AccountRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<SyncWalletsBackupPasswordRequester.EmptyRequest, SyncWalletsBackupPasswordResponder.Response>(navigationHolder),
    SyncWalletsBackupPasswordCommunicator {

    override fun openRequest(request: SyncWalletsBackupPasswordRequester.EmptyRequest) {
        super.openRequest(request)

        router.openSyncWalletsBackupPassword()
    }
}
