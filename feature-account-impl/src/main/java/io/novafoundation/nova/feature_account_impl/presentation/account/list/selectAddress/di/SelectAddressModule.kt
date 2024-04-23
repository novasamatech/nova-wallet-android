package io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountValidForTransactionListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress.SelectAddressViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectAddressModule {

    @Provides
    fun provideAccountListingMixinFactory(
        walletUiUseCase: WalletUiUseCase,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
        accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    ): MetaAccountValidForTransactionListingMixinFactory {
        return MetaAccountValidForTransactionListingMixinFactory(
            walletUiUseCase = walletUiUseCase,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            accountTypePresentationMapper = accountTypePresentationMapper
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(SelectAddressViewModel::class)
    fun provideViewModel(
        accountListingMixinFactory: MetaAccountValidForTransactionListingMixinFactory,
        router: AccountRouter,
        selectAddressCommunicator: SelectAddressCommunicator,
        accountInteractor: AccountInteractor,
        request: SelectAddressRequester.Request
    ): ViewModel {
        return SelectAddressViewModel(
            accountListingMixinFactory = accountListingMixinFactory,
            router = router,
            selectAddressResponder = selectAddressCommunicator,
            accountInteractor = accountInteractor,
            request = request
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectAddressViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectAddressViewModel::class.java)
    }
}
