package io.novafoundation.nova.feature_dapp_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.di.modules.BrowserTabsModule
import io.novafoundation.nova.feature_dapp_impl.di.modules.DappMetadataModule
import io.novafoundation.nova.feature_dapp_impl.di.modules.FavouritesDAppModule
import io.novafoundation.nova.feature_dapp_impl.di.modules.PhishingSitesModule
import io.novafoundation.nova.feature_dapp_impl.di.modules.Web3Module
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor

@Module(includes = [Web3Module::class, DappMetadataModule::class, PhishingSitesModule::class, FavouritesDAppModule::class, BrowserTabsModule::class])
class DappFeatureModule {

    @Provides
    @FeatureScope
    fun provideCommonInteractor(
        dAppMetadataRepository: DAppMetadataRepository,
        favouritesDAppRepository: FavouritesDAppRepository,
        phishingSitesRepository: PhishingSitesRepository,
        resourceManager: ResourceManager
    ) = DappInteractor(
        dAppMetadataRepository = dAppMetadataRepository,
        favouritesDAppRepository = favouritesDAppRepository,
        phishingSitesRepository = phishingSitesRepository,
        resourceManager = resourceManager
    )
}
