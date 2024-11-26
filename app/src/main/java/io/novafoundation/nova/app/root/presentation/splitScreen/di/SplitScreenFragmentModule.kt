package io.novafoundation.nova.app.root.presentation.splitScreen.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenViewModel
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule

@Module(
    includes = [
        ViewModelModule::class
    ]
)
class SplitScreenFragmentModule {

    @Provides
    @IntoMap
    @ViewModelKey(SplitScreenViewModel::class)
    fun provideViewModel(): ViewModel {
        return SplitScreenViewModel()
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SplitScreenViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SplitScreenViewModel::class.java)
    }
}
