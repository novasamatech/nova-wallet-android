package io.novafoundation.nova.feature_dapp_impl.di.modules.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_api.di.deeplinks.DAppDeepLinks
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.deeplink.DAppDeepLinkHandler

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideDappDeepLinkHandler(
        dAppMetadataRepository: DAppMetadataRepository,
        router: DAppRouter,
        automaticInteractionGate: AutomaticInteractionGate
    ) = DAppDeepLinkHandler(
        dAppMetadataRepository,
        router,
        automaticInteractionGate
    )

    @Provides
    @FeatureScope
    fun provideDeepLinks(dapp: DAppDeepLinkHandler): DAppDeepLinks {
        return DAppDeepLinks(listOf(dapp))
    }
}
