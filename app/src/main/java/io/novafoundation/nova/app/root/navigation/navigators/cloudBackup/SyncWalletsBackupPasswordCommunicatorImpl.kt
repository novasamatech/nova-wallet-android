package io.novafoundation.nova.app.root.navigation.navigators.cloudBackup

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordResponder

class SyncWalletsBackupPasswordCommunicatorImpl(private val router: AccountRouter, navigationHoldersRegistry: NavigationHoldersRegistry) :
    NavStackInterScreenCommunicator<SyncWalletsBackupPasswordRequester.EmptyRequest, SyncWalletsBackupPasswordResponder.Response>(navigationHoldersRegistry),
    SyncWalletsBackupPasswordCommunicator {

    override fun openRequest(request: SyncWalletsBackupPasswordRequester.EmptyRequest) {
        super.openRequest(request)

        router.openSyncWalletsBackupPassword()
    }
}
