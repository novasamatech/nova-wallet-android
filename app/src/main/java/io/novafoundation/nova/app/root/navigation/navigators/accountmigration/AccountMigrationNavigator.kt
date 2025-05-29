package io.novafoundation.nova.app.root.navigation.navigators.accountmigration

import android.os.Bundle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.delayedNavigation.NavComponentDelayedNavigation
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
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

    override fun openPinCodeSet() {
        val args = buildCreatePinBundle()

        navigationBuilder().action(R.id.action_migration_to_pin)
            .setArgs(args)
            .navigateInRoot()
    }

    private fun buildCreatePinBundle(): Bundle {
        val delayedNavigation = NavComponentDelayedNavigation(R.id.action_open_split_screen)
        val action = PinCodeAction.Create(delayedNavigation)
        return PincodeFragment.getPinCodeBundle(action)
    }
}
