package io.novafoundation.nova.feature_account_impl.presentation.account.management.di

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
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.management.WalletManagmentViewModel
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory

@Module(includes = [ViewModelModule::class])
class WalletManagmentModule {

    @Provides
    @IntoMap
    @ViewModelKey(WalletManagmentViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        resourceManager: ResourceManager,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        metaAccountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
        cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory,
        listSelectorMixinFactory: ListSelectorMixin.Factory
    ): ViewModel {
        return WalletManagmentViewModel(
            interactor,
            router,
            resourceManager,
            actionAwaitableMixinFactory,
            metaAccountListingMixinFactory,
            cloudBackupChangingWarningMixinFactory,
            listSelectorMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): WalletManagmentViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletManagmentViewModel::class.java)
    }
}
