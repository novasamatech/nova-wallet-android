package io.novafoundation.nova.feature_assets.presentation.balance.detail.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixin
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailViewModel
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.HistoryFiltersProvider
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class BalanceDetailModule {

    @Provides
    @ScreenScope
    fun provideTransferHistoryMixin(
        walletInteractor: WalletInteractor,
        addressIconGenerator: AddressIconGenerator,
        walletRouter: WalletRouter,
        historyFiltersProvider: HistoryFiltersProvider,
        resourceManager: ResourceManager,
        assetPayload: AssetPayload,
        addressDisplayUseCase: AddressDisplayUseCase,
        chainRegistry: ChainRegistry,
    ): TransactionHistoryMixin {
        return TransactionHistoryProvider(
            walletInteractor,
            addressIconGenerator,
            walletRouter,
            historyFiltersProvider,
            resourceManager,
            addressDisplayUseCase,
            chainRegistry,
            assetPayload.chainId,
            assetPayload.chainAssetId
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(BalanceDetailViewModel::class)
    fun provideViewModel(
        interactor: WalletInteractor,
        sendInteractor: SendInteractor,
        router: WalletRouter,
        transactionHistoryMixin: TransactionHistoryMixin,
        buyMixin: BuyMixin.Presentation,
        assetPayload: AssetPayload
    ): ViewModel {
        return BalanceDetailViewModel(interactor, sendInteractor, router, assetPayload, buyMixin, transactionHistoryMixin)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): BalanceDetailViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(BalanceDetailViewModel::class.java)
    }
}
