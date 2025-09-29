package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.fee.RealFeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.chains.AssetHubFeePaymentProviderFactory
import io.novafoundation.nova.feature_account_impl.data.fee.chains.DefaultFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.HydrationFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.HydraDxQuoteSharedComputation
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.RealHydrationFeeInjector
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.BlockNumberUpdater
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class CustomFeeModule {

    @Provides
    @FeatureScope
    fun provideBlockNumberUpdater(
        chainRegistry: ChainRegistry,
        storageCache: StorageCache
    ): BlockNumberUpdater {
        return BlockNumberUpdater(
            chainRegistry,
            storageCache
        )
    }

    @Provides
    @FeatureScope
    fun provideHydraDxQuoteSharedComputation(
        computationalCache: ComputationalCache,
        quotingFactory: HydraDxQuoting.Factory,
        pathQuoterFactory: PathQuoter.Factory,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
        chainStateRepository: ChainStateRepository
    ): HydraDxQuoteSharedComputation {
        return HydraDxQuoteSharedComputation(
            computationalCache = computationalCache,
            quotingFactory = quotingFactory,
            pathQuoterFactory = pathQuoterFactory,
            storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory,
            chainStateRepository = chainStateRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideHydraFeesInjector(
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    ): HydrationFeeInjector = RealHydrationFeeInjector(
        hydraDxAssetIdConverter,
    )

    @Provides
    @FeatureScope
    fun provideHydrationFeePaymentProvider(
        chainRegistry: ChainRegistry,
        hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation,
        hydrationFeeInjector: HydrationFeeInjector,
        accountRepository: AccountRepository,
        hydrationPriceConversionFallback: HydrationPriceConversionFallback
    ) = HydrationFeePaymentProvider(
        chainRegistry = chainRegistry,
        hydraDxQuoteSharedComputation = hydraDxQuoteSharedComputation,
        hydrationFeeInjector = hydrationFeeInjector,
        hydrationPriceConversionFallback = hydrationPriceConversionFallback,
        accountRepository = accountRepository,
    )

    @Provides
    @FeatureScope
    fun provideDefaultFeePaymentProvider() = DefaultFeePaymentProvider()

    @Provides
    @FeatureScope
    fun provideFeePaymentProviderRegistry(
        defaultFeePaymentProvider: DefaultFeePaymentProvider,
        assetHubFeePaymentProviderFactory: AssetHubFeePaymentProviderFactory,
        hydrationFeePaymentProvider: HydrationFeePaymentProvider
    ): FeePaymentProviderRegistry = RealFeePaymentProviderRegistry(
        defaultFeePaymentProvider,
        assetHubFeePaymentProviderFactory,
        hydrationFeePaymentProvider
    )
}
