package io.novafoundation.nova.feature_gift_impl.di.modules.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.DialogMessageManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_gift_api.di.GiftDeepLinks
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink.ClaimGiftDeepLinkConfigurator
import io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink.ClaimGiftDeepLinkHandler
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideDeepLinkConfigurator(
        linkBuilderFactory: LinkBuilderFactory
    ): ClaimGiftDeepLinkConfigurator {
        return ClaimGiftDeepLinkConfigurator(linkBuilderFactory)
    }

    @Provides
    @FeatureScope
    fun provideClaimGiftDeepLinkHandler(
        router: GiftRouter,
        chainRegistry: ChainRegistry,
        automaticInteractionGate: AutomaticInteractionGate,
        dialogMessageManager: DialogMessageManager,
        claimGiftDeepLinkConfigurator: ClaimGiftDeepLinkConfigurator,
        claimGiftInteractor: ClaimGiftInteractor
    ): ClaimGiftDeepLinkHandler {
        return ClaimGiftDeepLinkHandler(
            router,
            chainRegistry,
            automaticInteractionGate,
            dialogMessageManager,
            claimGiftDeepLinkConfigurator,
            claimGiftInteractor
        )
    }

    @Provides
    @FeatureScope
    fun provideDeepLinks(
        claimGiftDeepLinkInteractor: ClaimGiftDeepLinkHandler
    ): GiftDeepLinks {
        return GiftDeepLinks(listOf(claimGiftDeepLinkInteractor))
    }
}
