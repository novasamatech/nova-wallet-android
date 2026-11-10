package io.novafoundation.nova.app.root.presentation.main.swaps.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.app.root.presentation.main.swaps.SwapsTabViewModel
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule

@Module(includes = [ViewModelModule::class])
class SwapsTabModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapsTabViewModel::class)
    fun provideViewModel(): ViewModel = SwapsTabViewModel()

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): SwapsTabViewModel {
        return ViewModelProvider(fragment, factory).get(SwapsTabViewModel::class.java)
    }
}
