package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.nft.NftNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_nft_impl.NftRouter

@Module
class NftNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHoldersRegistry: NavigationHoldersRegistry): NftRouter =
        NftNavigator(navigationHoldersRegistry)
}
