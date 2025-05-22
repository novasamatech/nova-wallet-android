package io.novafoundation.nova.app.root.navigation.navigators.accountmigration

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_account_migration.presentation.pairing.AccountMigrationPairingFragment
import io.novafoundation.nova.feature_account_migration.presentation.pairing.AccountMigrationPairingPayload

class AccountMigrationNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), AccountMigrationRouter {

    override fun openAccountMigrationPairing(scheme: String) {
        val payload = AccountMigrationPairingPayload(scheme)
        navigationBuilder().action(R.id.action_open_accountMigrationPairing)
            .setArgs(AccountMigrationPairingFragment.Companion.createPayload(payload))
            .navigateInRoot()
    }

    override fun finishMigrationFlow() {
        navigationBuilder().action(R.id.action_open_split_screen)
            .navigateInRoot()
    }
}
