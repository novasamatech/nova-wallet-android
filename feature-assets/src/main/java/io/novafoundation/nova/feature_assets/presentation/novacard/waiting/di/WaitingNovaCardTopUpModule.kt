package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.waiting.WaitingNovaCardTopUpViewModel

@Module(includes = [ViewModelModule::class])
class WaitingNovaCardTopUpModule {

    @Provides
    @IntoMap
    @ViewModelKey(WaitingNovaCardTopUpViewModel::class)
    fun provideViewModel(
        assetsRouter: AssetsRouter,
        novaCardInteractor: NovaCardInteractor,
        resourceManager: ResourceManager
    ): ViewModel {
        return WaitingNovaCardTopUpViewModel(assetsRouter, resourceManager, novaCardInteractor)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): WaitingNovaCardTopUpViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WaitingNovaCardTopUpViewModel::class.java)
    }
}
