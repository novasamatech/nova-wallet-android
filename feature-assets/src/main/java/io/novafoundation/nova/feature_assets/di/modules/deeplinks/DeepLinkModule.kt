package io.novafoundation.nova.feature_assets.di.modules.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideDeepLinkConfigurator(
        linkBuilderFactory: LinkBuilderFactory
    ): AssetDetailsDeepLinkConfigurator {
        return AssetDetailsDeepLinkConfigurator(linkBuilderFactory)
    }

    @Provides
    @FeatureScope
    fun provideAssetDetailsDeepLinkHandler(
        router: AssetsRouter,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        automaticInteractionGate: AutomaticInteractionGate,
        assetDetailsDeepLinkConfigurator: AssetDetailsDeepLinkConfigurator
    ): AssetDetailsDeepLinkHandler {
        return AssetDetailsDeepLinkHandler(
            router,
            accountRepository,
            chainRegistry,
            automaticInteractionGate,
            assetDetailsDeepLinkConfigurator
        )
    }

    @Provides
    @FeatureScope
    fun provideDeepLinks(assetDetails: AssetDetailsDeepLinkHandler): AssetDeepLinks {
        return AssetDeepLinks(listOf(assetDetails))
    }
}
