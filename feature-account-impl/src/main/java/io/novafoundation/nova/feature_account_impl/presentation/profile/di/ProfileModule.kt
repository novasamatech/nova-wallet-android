package io.novafoundation.nova.feature_account_impl.presentation.profile.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.profile.ProfileViewModel

@Module(includes = [ViewModelModule::class])
class ProfileModule {

    @Provides
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        addressIconGenerator: AddressIconGenerator,
    ): ViewModel {
        return ProfileViewModel(
            interactor,
            router,
            addressIconGenerator
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ProfileViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ProfileViewModel::class.java)
    }
}
