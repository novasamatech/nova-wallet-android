package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_impl.data.repository.WatchOnlyRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novafoundation.nova.feature_account_impl.domain.watchOnly.create.CreateWatchWalletInteractor
import io.novafoundation.nova.feature_account_impl.domain.watchOnly.create.RealCreateWatchWalletInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create.CreateWatchWalletViewModel

@Module(includes = [ViewModelModule::class])
class CreateWatchWalletModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        watchOnlyRepository: WatchOnlyRepository,
        watchOnlyAddAccountRepository: WatchOnlyAddAccountRepository,
        accountRepository: AccountRepository
    ): CreateWatchWalletInteractor = RealCreateWatchWalletInteractor(watchOnlyRepository, watchOnlyAddAccountRepository, accountRepository)

    @Provides
    @IntoMap
    @ViewModelKey(CreateWatchWalletViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        addressInputMixinFactory: AddressInputMixinFactory,
        interactor: CreateWatchWalletInteractor,
        accountInteractor: AccountInteractor,
        resourceManager: ResourceManager
    ): ViewModel {
        return CreateWatchWalletViewModel(router, addressInputMixinFactory, interactor, accountInteractor, resourceManager)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): CreateWatchWalletViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CreateWatchWalletViewModel::class.java)
    }
}
