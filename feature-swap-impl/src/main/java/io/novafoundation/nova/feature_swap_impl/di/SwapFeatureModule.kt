package io.novafoundation.nova.feature_swap_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.crossChain.CrossChainTransferAssetExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.updaters.SwapUpdateSystemFactory
import io.novafoundation.nova.feature_swap_impl.data.repository.RealSwapTransactionHistoryRepository
import io.novafoundation.nova.feature_swap_impl.data.repository.SwapTransactionHistoryRepository
import io.novafoundation.nova.feature_swap_impl.di.exchanges.AssetConversionExchangeModule
import io.novafoundation.nova.feature_swap_impl.di.exchanges.CrossChainTransferExchangeModule
import io.novafoundation.nova.feature_swap_impl.di.exchanges.HydraDxExchangeModule
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.domain.RealAssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.domain.interactor.RealSwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import io.novafoundation.nova.feature_swap_impl.domain.swap.RealSwapService
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.CanReceiveAssetOutValidationFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.RealPriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.RealSwapRateFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.RealSwapConfirmationDetailsFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.SwapConfirmationDetailsFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.navigation.RealSwapFlowScopeAggregator
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.RealSwapRouteFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.RealSwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.RealSwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.updater.AccountInfoUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module(includes = [HydraDxExchangeModule::class, AssetConversionExchangeModule::class, CrossChainTransferExchangeModule::class])
class SwapFeatureModule {

    @FeatureScope
    @Provides
    fun provideSwapService(
        assetConversionFactory: AssetConversionExchangeFactory,
        hydraDxExchangeFactory: HydraDxExchangeFactory,
        crossChainTransferAssetExchangeFactory: CrossChainTransferAssetExchangeFactory,
        computationalCache: ComputationalCache,
        chainRegistry: ChainRegistry,
        quoterFactory: PathQuoter.Factory,
        extrinsicServiceFactory: ExtrinsicService.Factory,
        defaultFeePaymentRegistry: FeePaymentProviderRegistry,
        tokenRepository: TokenRepository,
        accountRepository: AccountRepository,
        assetSourceRegistry: AssetSourceRegistry,
        chainStateRepository: ChainStateRepository,
        signerProvider: SignerProvider
    ): SwapService {
        return RealSwapService(
            assetConversionFactory = assetConversionFactory,
            hydraDxExchangeFactory = hydraDxExchangeFactory,
            crossChainTransferFactory = crossChainTransferAssetExchangeFactory,
            computationalCache = computationalCache,
            chainRegistry = chainRegistry,
            quoterFactory = quoterFactory,
            extrinsicServiceFactory = extrinsicServiceFactory,
            defaultFeePaymentProviderRegistry = defaultFeePaymentRegistry,
            tokenRepository = tokenRepository,
            assetSourceRegistry = assetSourceRegistry,
            accountRepository = accountRepository,
            chainStateRepository = chainStateRepository,
            signerProvider = signerProvider
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
        priceImpactThresholds: PriceImpactThresholds,
        swapService: SwapService,
        tokenRepository: TokenRepository,
        swapUpdateSystemFactory: SwapUpdateSystemFactory,
        assetsValidationContextFactory: AssetsValidationContext.Factory,
        canReceiveAssetOutValidationFactory: CanReceiveAssetOutValidationFactory,
    ): SwapInteractor {
        return SwapInteractor(
            priceImpactThresholds = priceImpactThresholds,
            swapService = swapService,
            canReceiveAssetOutValidationFactory = canReceiveAssetOutValidationFactory,
            swapUpdateSystemFactory = swapUpdateSystemFactory,
            assetsValidationContextFactory = assetsValidationContextFactory,
            tokenRepository = tokenRepository
        )
    }

    @Provides
    @FeatureScope
    fun providePriceImpactThresholds() = PriceImpactThresholds(
        lowPriceImpact = 1.percents,
        mediumPriceImpact = 5.percents,
        highPriceImpact = 15.percents
    )

    @Provides
    @FeatureScope
    fun providePriceImpactFormatter(
        priceImpactThresholds: PriceImpactThresholds,
        resourceManager: ResourceManager
    ): PriceImpactFormatter {
        return RealPriceImpactFormatter(priceImpactThresholds, resourceManager)
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
    ): SwapSettingsStateProvider {
        return RealSwapSettingsStateProvider(computationalCache)
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
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
        accountInfoUpdaterFactory: AccountInfoUpdaterFactory
    ): SwapUpdateSystemFactory {
        return SwapUpdateSystemFactory(
            swapSettingsStateProvider = swapSettingsStateProvider,
            chainRegistry = chainRegistry,
            storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory,
            accountInfoUpdaterFactory = accountInfoUpdaterFactory
        )
    }

    @Provides
    @FeatureScope
    fun provideSwapQuoteStoreProvider(computationalCache: ComputationalCache): SwapStateStoreProvider {
        return RealSwapStateStoreProvider(computationalCache)
    }

    @Provides
    @FeatureScope
    fun provideSwapRouteFormatter(chainRegistry: ChainRegistry): SwapRouteFormatter {
        return RealSwapRouteFormatter(chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideConfirmationDetailsFormatter(
        chainRegistry: ChainRegistry,
        assetIconProvider: AssetIconProvider,
        tokenRepository: TokenRepository,
        swapRouteFormatter: SwapRouteFormatter,
        swapRateFormatter: SwapRateFormatter,
        priceImpactFormatter: PriceImpactFormatter,
        resourceManager: ResourceManager,
        amountFormatter: AmountFormatter
    ): SwapConfirmationDetailsFormatter {
        return RealSwapConfirmationDetailsFormatter(
            chainRegistry = chainRegistry,
            assetIconProvider = assetIconProvider,
            tokenRepository = tokenRepository,
            swapRouteFormatter = swapRouteFormatter,
            swapRateFormatter = swapRateFormatter,
            priceImpactFormatter = priceImpactFormatter,
            resourceManager = resourceManager,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    @FeatureScope
    fun provideAssetInAdditionalSwapDeductionUseCase(
        assetSourceRegistry: AssetSourceRegistry,
        chainRegistry: ChainRegistry
    ): AssetInAdditionalSwapDeductionUseCase {
        return RealAssetInAdditionalSwapDeductionUseCase(assetSourceRegistry, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideSwapFlowScopeAggregator(): SwapFlowScopeAggregator {
        return RealSwapFlowScopeAggregator()
    }
}
