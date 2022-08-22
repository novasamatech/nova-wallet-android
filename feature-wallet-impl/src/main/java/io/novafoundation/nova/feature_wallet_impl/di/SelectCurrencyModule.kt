package io.novafoundation.nova.feature_wallet_impl.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.currency.SelectCurrencyViewModel

@Module(includes = [ViewModelModule::class])
class SelectCurrencyModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectCurrencyViewModel::class)
    fun provideViewModel(
        currencyInteractor: CurrencyInteractor,
        resourceManager: ResourceManager,
        walletRouter: WalletRouter
    ): ViewModel {
        return SelectCurrencyViewModel(
            currencyInteractor,
            resourceManager,
            walletRouter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectCurrencyViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectCurrencyViewModel::class.java)
    }
}
