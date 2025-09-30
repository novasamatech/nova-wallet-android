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
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationInfoUseCase
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractorImpl
import io.novafoundation.nova.feature_assets.domain.price.ChartsInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellSelectorMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailViewModel
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.HistoryFiltersProviderFactory
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryProvider
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class BalanceDetailModule {

    @Provides
    @ScreenScope
    fun provideBalanceLocksInteractor(
        chainRegistry: ChainRegistry,
        balanceLocksRepository: BalanceLocksRepository,
        balanceHoldsRepository: BalanceHoldsRepository,
        accountRepository: AccountRepository
    ): BalanceLocksInteractor {
        return BalanceLocksInteractorImpl(
            chainRegistry = chainRegistry,
            balanceLocksRepository = balanceLocksRepository,
            balanceHoldsRepository = balanceHoldsRepository,
            accountRepository = accountRepository
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
        currencyRepository: CurrencyRepository,
        assetIconProvider: AssetIconProvider
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
            assetId = assetPayload.chainAssetId,
            currencyRepository = currencyRepository,
            assetIconProvider
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
        assetPayload: AssetPayload,
        accountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        currencyInteractor: CurrencyInteractor,
        controllableAssetCheckMixin: ControllableAssetCheckMixin,
        externalBalancesInteractor: ExternalBalancesInteractor,
        swapAvailabilityInteractor: SwapAvailabilityInteractor,
        assetIconProvider: AssetIconProvider,
        chartsInteractor: ChartsInteractor,
        buySellSelectorMixinFactory: BuySellSelectorMixinFactory,
        amountFormatter: AmountFormatter,
        chainMigrationInfoUseCase: ChainMigrationInfoUseCase
    ): ViewModel {
        return BalanceDetailViewModel(
            walletInteractor = walletInteractor,
            balanceLocksInteractor = balanceLocksInteractor,
            sendInteractor = sendInteractor,
            router = router,
            assetPayload = assetPayload,
            transactionHistoryMixin = transactionHistoryMixin,
            accountUseCase = accountUseCase,
            resourceManager = resourceManager,
            currencyInteractor = currencyInteractor,
            controllableAssetCheck = controllableAssetCheckMixin,
            externalBalancesInteractor = externalBalancesInteractor,
            swapAvailabilityInteractor = swapAvailabilityInteractor,
            assetIconProvider = assetIconProvider,
            chartsInteractor = chartsInteractor,
            buySellSelectorMixinFactory = buySellSelectorMixinFactory,
            amountFormatter = amountFormatter,
            chainMigrationInfoUseCase = chainMigrationInfoUseCase
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
