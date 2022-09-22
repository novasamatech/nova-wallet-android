package io.novafoundation.nova.feature_assets.presentation.balance.list.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdownInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractorImpl
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksRepository
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.list.BalanceListViewModel
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class BalanceListModule {

    @Provides
    @ScreenScope
    fun provideBalanceLocksInteractor(
        chainRegistry: ChainRegistry,
        balanceLocksRepository: BalanceLocksRepository
    ): BalanceLocksInteractor {
        return BalanceLocksInteractorImpl(
            chainRegistry,
            balanceLocksRepository
        )
    }

    @Provides
    @ScreenScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        nftRepository: NftRepository
    ) = AssetsListInteractor(accountRepository, nftRepository)

    @Provides
    @ScreenScope
    fun provideBalanceBreakdownInteractor(
        accountRepository: AccountRepository,
        balanceLocksRepository: BalanceLocksRepository,
        contributionsRepository: ContributionsRepository
    ): BalanceBreakdownInteractor {
        return BalanceBreakdownInteractor(
            accountRepository,
            balanceLocksRepository,
            contributionsRepository
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(BalanceListViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        assetsListInteractor: AssetsListInteractor,
        router: AssetsRouter,
        selectedAccountUseCase: SelectedAccountUseCase,
        currencyInteractor: CurrencyInteractor,
        balanceBreakdownInteractor: BalanceBreakdownInteractor,
        resourceManager: ResourceManager
    ): ViewModel {
        return BalanceListViewModel(
            interactor,
            assetsListInteractor,
            selectedAccountUseCase,
            router,
            currencyInteractor,
            balanceBreakdownInteractor,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): BalanceListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(BalanceListViewModel::class.java)
    }
}
