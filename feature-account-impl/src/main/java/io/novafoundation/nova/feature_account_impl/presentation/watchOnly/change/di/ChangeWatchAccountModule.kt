package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novafoundation.nova.feature_account_impl.domain.watchOnly.change.ChangeWatchAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.watchOnly.change.RealChangeWatchAccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.ChangeWatchAccountViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ChangeWatchAccountModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        watchOnlyAddAccountRepository: WatchOnlyAddAccountRepository
    ): ChangeWatchAccountInteractor = RealChangeWatchAccountInteractor(watchOnlyAddAccountRepository)

    @Provides
    @IntoMap
    @ViewModelKey(ChangeWatchAccountViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        addressInputMixinFactory: AddressInputMixinFactory,
        interactor: ChangeWatchAccountInteractor,
        chainRegistry: ChainRegistry,
        payload: AddAccountPayload.ChainAccount,
        resourceManager: ResourceManager
    ): ViewModel {
        return ChangeWatchAccountViewModel(
            router = router,
            addressInputMixinFactory = addressInputMixinFactory,
            chainRegistry = chainRegistry,
            interactor = interactor,
            payload = payload,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ChangeWatchAccountViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ChangeWatchAccountViewModel::class.java)
    }
}
