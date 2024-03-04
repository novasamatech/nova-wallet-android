package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet.SelectWalletViewModel

@Module(includes = [ViewModelModule::class])
class SelectWalletModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectWalletViewModel::class)
    fun provideViewModel(
        communicator: SelectWalletCommunicator,
        router: AccountRouter,
        accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
    ): ViewModel {
        return SelectWalletViewModel(
            router = router,
            accountListingMixinFactory = accountListingMixinFactory,
            responder = communicator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectWalletViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectWalletViewModel::class.java)
    }
}
