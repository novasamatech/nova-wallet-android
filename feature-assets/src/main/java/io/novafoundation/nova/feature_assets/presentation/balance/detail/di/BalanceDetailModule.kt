package io.novafoundation.nova.feature_assets.presentation.balance.detail.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractorImpl
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailViewModel
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.HistoryFiltersProviderFactory
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryProvider
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class BalanceDetailModule {

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
    fun provideTransferHistoryMixin(
        walletInteractor: WalletInteractor,
        assetsRouter: AssetsRouter,
        historyFiltersProviderFactory: HistoryFiltersProviderFactory,
        assetSourceRegistry: AssetSourceRegistry,
        resourceManager: ResourceManager,
        assetPayload: AssetPayload,
        addressDisplayUseCase: AddressDisplayUseCase,
        chainRegistry: ChainRegistry,
    ): TransactionHistoryMixin {
        return TransactionHistoryProvider(
            walletInteractor = walletInteractor,
            router = assetsRouter,
            historyFiltersProviderFactory = historyFiltersProviderFactory,
            resourceManager = resourceManager,
            addressDisplayUseCase = addressDisplayUseCase,
            assetsSourceRegistry = assetSourceRegistry,
            chainRegistry = chainRegistry,
            chainId = assetPayload.chainId,
            assetId = assetPayload.chainAssetId
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(BalanceDetailViewModel::class)
    fun provideViewModel(
        walletInteractor: WalletInteractor,
        balanceLocksInteractor: BalanceLocksInteractor,
        sendInteractor: SendInteractor,
        router: AssetsRouter,
        transactionHistoryMixin: TransactionHistoryMixin,
        buyMixinFactory: BuyMixinFactory,
        assetPayload: AssetPayload,
        accountUseCase: SelectedAccountUseCase,
        missingKeysPresenter: WatchOnlyMissingKeysPresenter,
        resourceManager: ResourceManager,
        currencyInteractor: CurrencyInteractor,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        contributionsInteractor: ContributionsInteractor
    ): ViewModel {
        return BalanceDetailViewModel(
            walletInteractor = walletInteractor,
            balanceLocksInteractor = balanceLocksInteractor,
            sendInteractor = sendInteractor,
            router = router,
            assetPayload = assetPayload,
            buyMixinFactory = buyMixinFactory,
            transactionHistoryMixin = transactionHistoryMixin,
            accountUseCase = accountUseCase,
            missingKeysPresenter = missingKeysPresenter,
            resourceManager = resourceManager,
            currencyInteractor = currencyInteractor,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            contributionsInteractor = contributionsInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): BalanceDetailViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(BalanceDetailViewModel::class.java)
    }
}
