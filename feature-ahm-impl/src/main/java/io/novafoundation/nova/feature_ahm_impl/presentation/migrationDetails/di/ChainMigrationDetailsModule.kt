package io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ahm_impl.domain.ChainMigrationDetailsInteractor
import io.novafoundation.nova.feature_ahm_impl.presentation.ChainMigrationRouter
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.ChainMigrationDetailsPayload
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.ChainMigrationDetailsViewModel
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory

@Module(includes = [ViewModelModule::class])
class ChainMigrationDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ChainMigrationDetailsViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: ChainMigrationRouter,
        interactor: ChainMigrationDetailsInteractor,
        payload: ChainMigrationDetailsPayload,
        promotionBannersMixinFactory: PromotionBannersMixinFactory,
        bannerSourceFactory: BannersSourceFactory,
    ): ViewModel {
        return ChainMigrationDetailsViewModel(
            resourceManager,
            router,
            interactor,
            payload,
            promotionBannersMixinFactory,
            bannerSourceFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ChainMigrationDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ChainMigrationDetailsViewModel::class.java)
    }
}
