package io.novafoundation.nova.app.root.navigation.navigators.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.navigationBuilder
import io.novafoundation.nova.feature_account_impl.presentation.seedScan.ScanSeedCommunicator

class ScanSeedCommunicatorImpl(
    private val navigationHoldersRegistry: NavigationHoldersRegistry
) : NavStackInterScreenCommunicator<ScanSeedCommunicator.Request, ScanSeedCommunicator.Response>(navigationHoldersRegistry),
    ScanSeedCommunicator {

    override fun openRequest(request: ScanSeedCommunicator.Request) {
        super.openRequest(request)

        navigationHoldersRegistry.navigationBuilder().action(R.id.action_scan_seed)
            .navigateInFirstAttachedContext()
    }
}
