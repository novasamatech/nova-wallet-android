package io.novafoundation.nova.feature_gift_impl.di.modules.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_gift_api.di.GiftDeepLinks
import io.novafoundation.nova.feature_gift_impl.presentation.share.deeplink.ShareGiftDeepLinkConfigurator

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideDeepLinkConfigurator(
        linkBuilderFactory: LinkBuilderFactory
    ): ShareGiftDeepLinkConfigurator {
        return ShareGiftDeepLinkConfigurator(linkBuilderFactory)
    }

    @Provides
    @FeatureScope
    fun provideDeepLinks(): GiftDeepLinks {
        // TODO: pass handlers here
        return GiftDeepLinks(emptyList())
    }
}
