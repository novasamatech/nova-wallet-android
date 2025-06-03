package io.novafoundation.nova.feature_buy_impl.di.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_buy_api.di.deeplinks.BuyDeepLinks
import io.novafoundation.nova.feature_buy_impl.presentation.deeplink.BuyCallbackDeepLinkHandler

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideBuyCallbackDeepLinkHandler(
        resourceManager: ResourceManager
    ) = BuyCallbackDeepLinkHandler(resourceManager)

    @Provides
    @FeatureScope
    fun provideDeepLinks(buyCallback: BuyCallbackDeepLinkHandler): BuyDeepLinks {
        return BuyDeepLinks(listOf(buyCallback))
    }
}
