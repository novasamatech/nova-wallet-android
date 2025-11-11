package io.novafoundation.nova.feature_account_impl.presentation.account.list.singleSelecting.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountValidForTransactionListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.singleSelecting.SelectSingleWalletViewModel

@Module(includes = [ViewModelModule::class])
class SelectSingleWalletModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectSingleWalletViewModel::class)
    fun provideViewModel(
        accountListingMixinFactory: MetaAccountValidForTransactionListingMixinFactory,
        router: AccountRouter,
        selectSingleWalletCommunicator: SelectSingleWalletCommunicator,
        request: SelectSingleWalletRequester.Request
    ): ViewModel {
        return SelectSingleWalletViewModel(
            accountListingMixinFactory = accountListingMixinFactory,
            router = router,
            selectSingleWalletResponder = selectSingleWalletCommunicator,
            request = request
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectSingleWalletViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectSingleWalletViewModel::class.java)
    }
}
