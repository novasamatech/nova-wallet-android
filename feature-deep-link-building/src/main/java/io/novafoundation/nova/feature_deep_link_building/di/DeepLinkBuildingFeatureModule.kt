package io.novafoundation.nova.feature_deep_link_building.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsDeepLinkConfigurator

@Module()
class DeepLinkBuildingFeatureModule {

    @Provides
    fun provideAssetDetailsDeepLinkConfigurator(
        resourceManager: ResourceManager
    ) = io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator(
        resourceManager = resourceManager
    )

    @Provides
    fun provideReferendumDetailsDeepLinkConfigurator(
        resourceManager: ResourceManager
    ) = io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsDeepLinkConfigurator(
        resourceManager = resourceManager
    )
}
