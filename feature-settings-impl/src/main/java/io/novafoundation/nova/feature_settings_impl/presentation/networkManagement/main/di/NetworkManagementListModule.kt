package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.NetworkManagementListViewModel

@Module(includes = [ViewModelModule::class])
class NetworkManagementListModule {

    @Provides
    @IntoMap
    @ViewModelKey(NetworkManagementListViewModel::class)
    fun provideViewModel(
        router: SettingsRouter,
        resourceManager: ResourceManager
    ): ViewModel {
        return NetworkManagementListViewModel(
            router,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): NetworkManagementListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NetworkManagementListViewModel::class.java)
    }
}
