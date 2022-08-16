package io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletRequester
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting.SelectWalletViewModel

@Module(includes = [ViewModelModule::class])
class SelectWalletModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectWalletViewModel::class)
    fun provideViewModel(
        accountListingMixinFactory: MetaAccountListingMixinFactory,
        router: AccountRouter,
        selectWalletCommunicator: SelectWalletCommunicator,
        accountInteractor: AccountInteractor,
        request: SelectWalletRequester.Request
    ): ViewModel {
        return SelectWalletViewModel(
            accountListingMixinFactory = accountListingMixinFactory,
            router = router,
            selectWalletResponder = selectWalletCommunicator,
            accountInteractor = accountInteractor,
            request = request
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
