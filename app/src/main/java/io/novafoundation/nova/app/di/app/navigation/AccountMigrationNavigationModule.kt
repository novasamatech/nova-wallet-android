package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.accountmigration.AccountMigrationNavigator
import io.novafoundation.nova.app.root.navigation.navigators.deepLinking.DeepLinkingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter

@Module
class AccountMigrationNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): AccountMigrationRouter = AccountMigrationNavigator(
        navigationHoldersRegistry = navigationHoldersRegistry
    )
}
