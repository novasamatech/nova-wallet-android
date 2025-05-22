package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.deepLinking.DeepLinkingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter

@Module
class DeepLinkingNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        accountRouter: AccountRouter,
        assetsRouter: AssetsRouter,
        dAppRouter: DAppRouter,
        accountMigrationRouter: AccountMigrationRouter
    ): DeepLinkingRouter = DeepLinkingNavigator(
        navigationHoldersRegistry = navigationHoldersRegistry,
        accountRouter = accountRouter,
        assetsRouter = assetsRouter,
        dAppRouter = dAppRouter,
        accountMigrationRouter = accountMigrationRouter
    )
}
