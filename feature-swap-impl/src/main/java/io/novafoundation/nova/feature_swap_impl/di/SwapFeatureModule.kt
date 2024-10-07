package io.novafoundation.nova.feature_swap_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.updaters.SwapUpdateSystemFactory
import io.novafoundation.nova.feature_swap_impl.data.repository.RealSwapTransactionHistoryRepository
import io.novafoundation.nova.feature_swap_impl.data.repository.SwapTransactionHistoryRepository
import io.novafoundation.nova.feature_swap_impl.di.exchanges.AssetConversionExchangeModule
import io.novafoundation.nova.feature_swap_impl.di.exchanges.HydraDxExchangeModule
import io.novafoundation.nova.feature_swap_impl.domain.interactor.RealSwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.swap.RealSwapService
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.RealPriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.RealSwapRateFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.RealSwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_swap_impl.presentation.state.RealSwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.updater.AccountInfoUpdaterFactory
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [HydraDxExchangeModule::class, AssetConversionExchangeModule::class])
class SwapFeatureModule {

    @FeatureScope
    @Provides
    fun provideSwapService(
        assetConversionFactory: AssetConversionExchangeFactory,
        hydraDxExchangeFactory: HydraDxExchangeFactory,
        computationalCache: ComputationalCache,
        chainRegistry: ChainRegistry,
        quoterFactory: PathQuoter.Factory,
        customFeeCapabilityFacade: CustomFeeCapabilityFacade
    ): SwapService {
        return RealSwapService(
            assetConversionFactory = assetConversionFactory,
            hydraDxExchangeFactory = hydraDxExchangeFactory,
            computationalCache = computationalCache,
            chainRegistry = chainRegistry,
            quoterFactory = quoterFactory,
            customFeeCapabilityFacade = customFeeCapabilityFacade
        )
    }

    @Provides
    @FeatureScope
    fun provideSwapAvailabilityInteractor(chainRegistry: ChainRegistry, swapService: SwapService): SwapAvailabilityInteractor {
        return RealSwapAvailabilityInteractor(chainRegistry, swapService)
    }

    @Provides
    @FeatureScope
    fun provideSwapTransactionHistoryRepository(
        operationDao: OperationDao,
        chainRegistry: ChainRegistry,
    ): SwapTransactionHistoryRepository {
        return RealSwapTransactionHistoryRepository(operationDao, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideSwapInteractor(
        swapService: SwapService,
        assetSourceRegistry: AssetSourceRegistry,
        chainRegistry: ChainRegistry,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        buyTokenRegistry: BuyTokenRegistry,
        crossChainTransfersUseCase: CrossChainTransfersUseCase,
        swapTransactionHistoryRepository: SwapTransactionHistoryRepository,
        swapUpdateSystemFactory: SwapUpdateSystemFactory
    ): SwapInteractor {
        return SwapInteractor(
            swapService = swapService,
            buyTokenRegistry = buyTokenRegistry,
            crossChainTransfersUseCase = crossChainTransfersUseCase,
            assetSourceRegistry = assetSourceRegistry,
            accountRepository = accountRepository,
            chainRegistry = chainRegistry,
            swapTransactionHistoryRepository = swapTransactionHistoryRepository,
            walletRepository = walletRepository,
            swapUpdateSystemFactory = swapUpdateSystemFactory
        )
    }

    @Provides
    @FeatureScope
    fun providePriceImpactFormatter(resourceManager: ResourceManager): PriceImpactFormatter {
        return RealPriceImpactFormatter(resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideSwapRateFormatter(): SwapRateFormatter {
        return RealSwapRateFormatter()
    }

    @Provides
    @FeatureScope
    fun provideSlippageAlertMixinFactory(resourceManager: ResourceManager): SlippageAlertMixinFactory {
        return SlippageAlertMixinFactory(resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideSwapSettingsStateProvider(
        computationalCache: ComputationalCache,
        chainRegistry: ChainRegistry
    ): SwapSettingsStateProvider {
        return RealSwapSettingsStateProvider(computationalCache, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideAccountInfoUpdaterFactory(
        storageCache: StorageCache,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry
    ): AccountInfoUpdaterFactory {
        return AccountInfoUpdaterFactory(
            storageCache,
            accountRepository,
            chainRegistry
        )
    }

    @Provides
    @FeatureScope
    fun provideSwapUpdateSystemFactory(
        swapSettingsStateProvider: SwapSettingsStateProvider,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
        accountInfoUpdaterFactory: AccountInfoUpdaterFactory
    ): SwapUpdateSystemFactory {
        return SwapUpdateSystemFactory(
            swapSettingsStateProvider = swapSettingsStateProvider,
            chainRegistry = chainRegistry,
            storageCache = storageCache,
            storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory,
            accountInfoUpdaterFactory = accountInfoUpdaterFactory
        )
    }

    @Provides
    @FeatureScope
    fun provideMaxActionProviderFactory(
        assetSourceRegistry: AssetSourceRegistry,
        chainRegistry: ChainRegistry,
    ): MaxActionProviderFactory {
        return MaxActionProviderFactory(
            assetSourceRegistry = assetSourceRegistry,
            chainRegistry = chainRegistry
        )
    }

    @Provides
    @FeatureScope
    fun provideSwapQuoteStoreProvider(computationalCache: ComputationalCache): SwapStateStoreProvider {
        return RealSwapStateStoreProvider(computationalCache)
    }
}
