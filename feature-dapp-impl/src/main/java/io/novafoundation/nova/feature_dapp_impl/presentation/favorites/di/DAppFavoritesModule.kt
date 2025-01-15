package io.novafoundation.nova.feature_dapp_impl.presentation.favorites.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.favorites.DAppFavoritesViewModel

@Module(includes = [ViewModelModule::class])
class DAppFavoritesModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): DAppFavoritesViewModel {
        return ViewModelProvider(fragment, factory).get(DAppFavoritesViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(DAppFavoritesViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        interactor: DappInteractor,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    ): ViewModel {
        return DAppFavoritesViewModel(
            router,
            interactor,
            actionAwaitableMixinFactory
        )
    }
}
