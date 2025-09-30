package io.novafoundation.nova.app.root.navigation.navigators.chainMigration

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_ahm_impl.presentation.ChainMigrationRouter
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.ChainMigrationDetailsFragment
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.ChainMigrationDetailsPayload

class ChainMigrationNavigator(navigationHoldersRegistry: NavigationHoldersRegistry) : ChainMigrationRouter, BaseNavigator(navigationHoldersRegistry) {

    override fun openChainMigrationDetails(chainId: String) {
        navigationBuilder().action(R.id.action_open_chain_migration_details)
            .setArgs(ChainMigrationDetailsFragment.createPayload(ChainMigrationDetailsPayload(chainId)))
            .navigateInRoot()
    }
}
