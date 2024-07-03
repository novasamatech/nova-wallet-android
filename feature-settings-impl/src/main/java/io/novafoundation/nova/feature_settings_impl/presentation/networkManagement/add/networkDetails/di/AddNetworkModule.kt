package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.di

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
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AddNetworkPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails.AddNetworkViewModel

@Module(includes = [ViewModelModule::class])
class AddNetworkModule {

    @Provides
    @IntoMap
    @ViewModelKey(AddNetworkViewModel::class)
    fun provideViewModel(
        router: SettingsRouter,
        addNetworkPayload: AddNetworkPayload
    ): ViewModel {
        return AddNetworkViewModel(
            router,
            addNetworkPayload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddNetworkViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddNetworkViewModel::class.java)
    }
}
