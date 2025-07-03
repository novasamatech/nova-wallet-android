package io.novafoundation.nova.feature_assets.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_assets.data.network.BalancesUpdateSystem
import io.novafoundation.nova.feature_assets.data.repository.NovaCardStateRepository
import io.novafoundation.nova.feature_assets.data.repository.RealNovaCardStateRepository
import io.novafoundation.nova.feature_assets.data.repository.RealTransactionHistoryRepository
import io.novafoundation.nova.feature_assets.data.repository.TransactionHistoryRepository
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.AssetFiltersRepository
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.PreferencesAssetFiltersRepository
import io.novafoundation.nova.feature_assets.di.modules.AddTokenModule
import io.novafoundation.nova.feature_assets.di.modules.ManageTokensCommonModule
import io.novafoundation.nova.feature_assets.di.modules.SendModule
import io.novafoundation.nova.feature_assets.di.modules.deeplinks.DeepLinkModule
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.WalletInteractorImpl
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.RealExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.domain.novaCard.RealNovaCardInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchUseCase
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetViewModeAssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.domain.networks.AssetNetworksInteractor
import io.novafoundation.nova.feature_assets.domain.price.ChartsInteractor
import io.novafoundation.nova.feature_assets.domain.price.RealChartsInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.ExpandableAssetsMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellSelectorMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.MultisigRestrictionCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.MultisigRestrictionCheckMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.multisig.RealMultisigRestrictionCheckMixin
import io.novafoundation.nova.feature_assets.presentation.swap.executor.InitialSwapFlowExecutor
import io.novafoundation.nova.feature_assets.presentation.swap.executor.SwapFlowExecutorFactory
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.HistoryFiltersProviderFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_staking_api.data.mythos.MythosMainPotMatcherFactory
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.RealAmountFormatter
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(
    includes = [
        SendModule::class,
        ManageTokensCommonModule::class,
        AddTokenModule::class,
        DeepLinkModule::class
    ]
)
class AssetsFeatureModule {

    @Provides
    @FeatureScope
    fun provideExternalBalancesInteractor(
        accountRepository: AccountRepository,
        externalBalanceRepository: ExternalBalanceRepository
    ): ExternalBalancesInteractor = RealExternalBalancesInteractor(accountRepository, externalBalanceRepository)

    @Provides
    @FeatureScope
    fun provideAssetSearchUseCase(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        swapService: SwapService
    ) = AssetSearchUseCase(walletRepository, accountRepository, chainRegistry, swapService)

    @Provides
    @FeatureScope
    fun provideSearchInteractorFactory(
        assetViewModeRepository: AssetsViewModeRepository,
        assetSearchUseCase: AssetSearchUseCase,
        chainRegistry: ChainRegistry,
        tradeTokenRegistry: TradeTokenRegistry
    ): AssetSearchInteractorFactory = AssetViewModeAssetSearchInteractorFactory(assetViewModeRepository, assetSearchUseCase, chainRegistry, tradeTokenRegistry)

    @Provides
    @FeatureScope
    fun provideAssetNetworksInteractor(
        chainRegistry: ChainRegistry,
        assetSearchUseCase: AssetSearchUseCase,
        tradeTokenRegistry: TradeTokenRegistry
    ) = AssetNetworksInteractor(chainRegistry, assetSearchUseCase, tradeTokenRegistry)

    @Provides
    @FeatureScope
    fun provideAssetFiltersRepository(preferences: Preferences): AssetFiltersRepository {
        return PreferencesAssetFiltersRepository(preferences)
    }

    @Provides
    @FeatureScope
    fun provideWalletInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        assetFiltersRepository: AssetFiltersRepository,
        chainRegistry: ChainRegistry,
        nftRepository: NftRepository,
        transactionHistoryRepository: TransactionHistoryRepository,
        currencyRepository: CurrencyRepository
    ): WalletInteractor = WalletInteractorImpl(
        walletRepository = walletRepository,
        accountRepository = accountRepository,
        assetFiltersRepository = assetFiltersRepository,
        chainRegistry = chainRegistry,
        nftRepository = nftRepository,
        transactionHistoryRepository = transactionHistoryRepository,
        currencyRepository = currencyRepository
    )

    @Provides
    @FeatureScope
    fun provideHistoryFiltersProviderFactory(
        computationalCache: ComputationalCache,
        assetSourceRegistry: AssetSourceRegistry,
        chainRegistry: ChainRegistry,
    ) = HistoryFiltersProviderFactory(computationalCache, assetSourceRegistry, chainRegistry)

    @Provides
    @FeatureScope
    fun provideControllableAssetCheckMixin(
        missingKeysPresenter: WatchOnlyMissingKeysPresenter,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        resourceManager: ResourceManager
    ): ControllableAssetCheckMixin {
        return ControllableAssetCheckMixin(
            missingKeysPresenter,
            actionAwaitableMixinFactory,
            resourceManager
        )
    }

    @Provides
    @FeatureScope
    fun provideBalancesUpdateSystem(
        chainRegistry: ChainRegistry,
        paymentUpdaterFactory: PaymentUpdaterFactory,
        balanceLocksUpdater: BalanceLocksUpdaterFactory,
        pooledBalanceUpdaterFactory: PooledBalanceUpdaterFactory,
        accountUpdateScope: AccountUpdateScope,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    ): BalancesUpdateSystem {
        return BalancesUpdateSystem(
            chainRegistry = chainRegistry,
            paymentUpdaterFactory = paymentUpdaterFactory,
            balanceLocksUpdater = balanceLocksUpdater,
            pooledBalanceUpdaterFactory = pooledBalanceUpdaterFactory,
            accountUpdateScope = accountUpdateScope,
            storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory
        )
    }

    @Provides
    @FeatureScope
    fun provideTransactionHistoryRepository(
        assetSourceRegistry: AssetSourceRegistry,
        operationsDao: OperationDao,
        coinPriceRepository: CoinPriceRepository,
        poolAccountDerivation: PoolAccountDerivation,
        mythosMainPotMatcherFactory: MythosMainPotMatcherFactory,
    ): TransactionHistoryRepository = RealTransactionHistoryRepository(
        assetSourceRegistry = assetSourceRegistry,
        operationDao = operationsDao,
        coinPriceRepository = coinPriceRepository,
        poolAccountDerivation = poolAccountDerivation,
        mythosMainPotMatcherFactory = mythosMainPotMatcherFactory
    )

    @Provides
    @FeatureScope
    fun provideNovaCardRepository(preferences: Preferences): NovaCardStateRepository {
        return RealNovaCardStateRepository(preferences)
    }

    @Provides
    @FeatureScope
    fun provideNovaCardInteractor(repository: NovaCardStateRepository): NovaCardInteractor {
        return RealNovaCardInteractor(repository)
    }

    @Provides
    @FeatureScope
    fun provideInitialSwapFlowExecutor(
        assetsRouter: AssetsRouter
    ): InitialSwapFlowExecutor {
        return InitialSwapFlowExecutor(assetsRouter)
    }

    @Provides
    @FeatureScope
    fun provideSwapExecutor(
        initialSwapFlowExecutor: InitialSwapFlowExecutor,
        assetsRouter: AssetsRouter,
        swapSettingsStateProvider: SwapSettingsStateProvider
    ): SwapFlowExecutorFactory {
        return SwapFlowExecutorFactory(initialSwapFlowExecutor, assetsRouter, swapSettingsStateProvider)
    }

    @Provides
    @FeatureScope
    fun provideAmountFormatter(resourceManager: ResourceManager): AmountFormatter {
        return RealAmountFormatter(resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideExpandableAssetsMixinFactory(
        assetIconProvider: AssetIconProvider,
        currencyInteractor: CurrencyInteractor,
        assetsViewModeRepository: AssetsViewModeRepository,
        amountFormatter: AmountFormatter
    ): ExpandableAssetsMixinFactory {
        return ExpandableAssetsMixinFactory(
            assetIconProvider,
            currencyInteractor,
            assetsViewModeRepository,
            amountFormatter
        )
    }

    @Provides
    @FeatureScope
    fun provideChartsInteractor(
        coinPriceRepository: CoinPriceRepository,
        currencyRepository: CurrencyRepository
    ): ChartsInteractor {
        return RealChartsInteractor(coinPriceRepository, currencyRepository)
    }

    @Provides
    @FeatureScope
    fun provideMultisigCheckMixinFactory(
        accountUseCase: SelectedAccountUseCase,
        actionLauncherFactory: ActionBottomSheetLauncherFactory,
        resourceManager: ResourceManager
    ): MultisigRestrictionCheckMixin {
        return RealMultisigRestrictionCheckMixin(
            accountUseCase,
            resourceManager,
            actionLauncherFactory.create()
        )
    }

    @Provides
    @FeatureScope
    fun provideBuySellMixinFactory(
        router: AssetsRouter,
        tradeTokenRegistry: TradeTokenRegistry,
        chainRegistry: ChainRegistry,
        resourceManager: ResourceManager,
        multisigRestrictionCheckMixin: MultisigRestrictionCheckMixin
    ): BuySellSelectorMixinFactory {
        return BuySellSelectorMixinFactory(
            router,
            tradeTokenRegistry,
            chainRegistry,
            resourceManager,
            multisigRestrictionCheckMixin
        )
    }
}
