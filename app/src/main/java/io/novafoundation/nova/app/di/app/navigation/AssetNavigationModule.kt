package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.topup.TopUpAddressCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressCommunicator

@Module
class AssetNavigationModule {

    @ApplicationScope
    @Provides
    fun provideTopUpAddressCommunicator(navigationHoldersRegistry: NavigationHoldersRegistry): TopUpAddressCommunicator {
        return TopUpAddressCommunicatorImpl(navigationHoldersRegistry)
    }
}
