package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.app.root.navigation.nft.NftNavigator
import io.novafoundation.nova.app.root.navigation.versions.VersionsNavigator
import io.novafoundation.nova.app.root.navigation.vote.VoteNavigatorFactory
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

@Module
class VersionsNavigationModule {

    @Provides
    @ApplicationScope
    fun provideRouter(navigationHolder: NavigationHolder): VersionsRouter = VersionsNavigator(navigationHolder)
}
