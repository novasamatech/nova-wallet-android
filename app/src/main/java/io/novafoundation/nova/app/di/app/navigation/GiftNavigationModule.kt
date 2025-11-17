package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.gift.GiftNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter

@Module
class GiftNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(commonDelegate: Navigator, navigationHoldersRegistry: NavigationHoldersRegistry): GiftRouter =
        GiftNavigator(commonDelegate, navigationHoldersRegistry)
}
