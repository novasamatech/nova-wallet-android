package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkMainViewModel

@Module(includes = [ViewModelModule::class])
class AddNetworkMainModule {

    @Provides
    @IntoMap
    @ViewModelKey(AddNetworkMainViewModel::class)
    fun provideViewModel(
        router: SettingsRouter
    ): ViewModel {
        return AddNetworkMainViewModel(
            router
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddNetworkMainViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddNetworkMainViewModel::class.java)
    }
}
