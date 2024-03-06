package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.versions.VersionsNavigator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter

@Module
class VersionsNavigationModule {

    @Provides
    @ApplicationScope
    fun provideRouter(
        navigationHolder: NavigationHolder,
        appLinksProvider: AppLinksProvider
    ): VersionsRouter = VersionsNavigator(navigationHolder, appLinksProvider.storeUrl)
}
