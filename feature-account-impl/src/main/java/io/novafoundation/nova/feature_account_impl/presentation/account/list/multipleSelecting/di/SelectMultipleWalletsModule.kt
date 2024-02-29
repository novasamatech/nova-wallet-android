package io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsRequester
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting.SelectMultipleWalletsViewModel

@Module(includes = [ViewModelModule::class])
class SelectMultipleWalletsModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectMultipleWalletsViewModel::class)
    fun provideViewModel(
        accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
        router: AccountRouter,
        selectMultipleWalletsCommunicator: SelectMultipleWalletsCommunicator,
        request: SelectMultipleWalletsRequester.Request,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        resourceManager: ResourceManager
    ): ViewModel {
        return SelectMultipleWalletsViewModel(
            accountListingMixinFactory = accountListingMixinFactory,
            router = router,
            responder = selectMultipleWalletsCommunicator,
            request = request,
            resourceManager = resourceManager,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectMultipleWalletsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectMultipleWalletsViewModel::class.java)
    }
}
