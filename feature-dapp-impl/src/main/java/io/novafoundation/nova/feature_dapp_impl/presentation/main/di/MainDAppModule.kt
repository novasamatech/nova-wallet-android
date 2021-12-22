package io.novafoundation.nova.feature_dapp_impl.presentation.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.main.MainDAppViewModel

@Module(includes = [ViewModelModule::class])
class MainDAppModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): MainDAppViewModel {
        return ViewModelProvider(fragment, factory).get(MainDAppViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(MainDAppViewModel::class)
    fun provideViewModel(
        addressIconGenerator: AddressIconGenerator,
        selectedAccountUseCase: SelectedAccountUseCase,
        router: DAppRouter
    ): ViewModel {
        return MainDAppViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }
}
