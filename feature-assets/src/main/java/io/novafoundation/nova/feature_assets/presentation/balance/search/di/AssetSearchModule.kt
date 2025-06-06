package io.novafoundation.nova.feature_assets.presentation.balance.search.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ExpandableAssetsMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.search.AssetSearchViewModel

@Module(includes = [ViewModelModule::class])
class AssetSearchModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetSearchViewModel {
        return ViewModelProvider(fragment, factory).get(AssetSearchViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetSearchViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        interactorFactory: AssetSearchInteractorFactory,
        externalBalancesInteractor: ExternalBalancesInteractor,
        expandableAssetsMixinFactory: ExpandableAssetsMixinFactory
    ): ViewModel {
        return AssetSearchViewModel(
            router = router,
            interactorFactory = interactorFactory,
            externalBalancesInteractor = externalBalancesInteractor,
            expandableAssetsMixinFactory = expandableAssetsMixinFactory
        )
    }
}
