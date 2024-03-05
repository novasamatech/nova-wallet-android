package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.versions.VersionsNavigator
import io.novafoundation.nova.common.di.modules.StoreLink
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter

@Module
class VersionsNavigationModule {

    @Provides
    @ApplicationScope
    fun provideRouter(
        navigationHolder: NavigationHolder,
        @StoreLink storeLink: String
    ): VersionsRouter = VersionsNavigator(navigationHolder, storeLink)
}
