package io.novafoundation.nova.feature_pay_impl.presentation.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_pay_impl.presentation.main.PayMainViewModel
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory

@Module(includes = [ViewModelModule::class])
class PayMainModule {

    @Provides
    @IntoMap
    @ViewModelKey(PayMainViewModel::class)
    fun provideViewModel(
        router: PayRouter,
        selectedAccountUseCase: SelectedAccountUseCase,
        walletConnectSessionsMixinFactory: WalletConnectSessionsMixinFactory
    ): ViewModel {
        return PayMainViewModel(
            router,
            selectedAccountUseCase,
            walletConnectSessionsMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PayMainViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PayMainViewModel::class.java)
    }
}
