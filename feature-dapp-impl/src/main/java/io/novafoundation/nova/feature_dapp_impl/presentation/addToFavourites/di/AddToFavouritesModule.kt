package io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_api.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.domain.browser.addToFavourites.AddToFavouritesInteractor
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.AddToFavouritesViewModel

@Module(includes = [ViewModelModule::class])
class AddToFavouritesModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        favouritesDAppRepository: FavouritesDAppRepository,
        dAppMetadataRepository: DAppMetadataRepository
    ) = AddToFavouritesInteractor(favouritesDAppRepository, dAppMetadataRepository)

    @Provides
    @ScreenScope
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AddToFavouritesViewModel {
        return ViewModelProvider(fragment, factory).get(AddToFavouritesViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AddToFavouritesViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        interactor: AddToFavouritesInteractor,
        payload: AddToFavouritesPayload
    ): ViewModel {
        return AddToFavouritesViewModel(
            router = router,
            interactor = interactor,
            payload = payload
        )
    }
}
