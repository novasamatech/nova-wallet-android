package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.chainMigration.ChainMigrationNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_ahm_impl.presentation.ChainMigrationRouter

@Module
class ChainMigrationNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHoldersRegistry: NavigationHoldersRegistry): ChainMigrationRouter =
        ChainMigrationNavigator(navigationHoldersRegistry)
}
