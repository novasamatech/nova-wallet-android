package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapConfirmationViewModel

@Module(includes = [ViewModelModule::class])
class SwapConfirmationModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapConfirmationViewModel::class)
    fun provideViewModel(resourceManager: ResourceManager): ViewModel {
        return SwapConfirmationViewModel(resourceManager)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapConfirmationViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapConfirmationViewModel::class.java)
    }
}
