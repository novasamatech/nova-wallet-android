package io.novafoundation.nova.feature_deep_link_building.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_deep_link_building.presentation.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_deep_link_building.presentation.ReferendumDetailsDeepLinkConfigurator

@Module()
class DeepLinkBuildingFeatureModule {

    @Provides
    fun provideAssetDetailsDeepLinkConfigurator(
        resourceManager: ResourceManager
    ) = AssetDetailsDeepLinkConfigurator(
        resourceManager = resourceManager
    )

    @Provides
    fun provideReferendumDetailsDeepLinkConfigurator(
        resourceManager: ResourceManager
    ) = ReferendumDetailsDeepLinkConfigurator(
        resourceManager = resourceManager
    )
}
