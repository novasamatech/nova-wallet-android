package io.novafoundation.nova.feature_wallet_impl.presentation.balance.filters.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_wallet_impl.data.repository.assetFilters.AssetFiltersRepository
import io.novafoundation.nova.feature_wallet_impl.domain.assets.filters.AssetFiltersInteractor
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.filters.AssetFiltersViewModel

@Module(includes = [ViewModelModule::class])
class AssetFiltersModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        assetFiltersRepository: AssetFiltersRepository
    ) = AssetFiltersInteractor(assetFiltersRepository)

    @Provides
    @IntoMap
    @ViewModelKey(AssetFiltersViewModel::class)
    fun provideViewModel(
        interactor: AssetFiltersInteractor,
        router: WalletRouter
    ): ViewModel {
        return AssetFiltersViewModel(
            interactor = interactor,
            router = router
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): AssetFiltersViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AssetFiltersViewModel::class.java)
    }
}
