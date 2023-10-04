package io.novafoundation.nova.feature_swap_impl.presentation.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_swap_api.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsViewModel

@Module(includes = [ViewModelModule::class])
class SwapMainSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapMainSettingsViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter
    ): ViewModel {
        return SwapMainSettingsViewModel(swapRouter)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapMainSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapMainSettingsViewModel::class.java)
    }
}
