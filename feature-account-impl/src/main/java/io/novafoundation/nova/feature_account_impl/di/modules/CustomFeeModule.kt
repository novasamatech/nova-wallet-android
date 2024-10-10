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
import io.novafoundation.nova.feature_account_impl.data.fee.chains.AssetHubFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.DefaultFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.HydrationFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.HydraDxQuoteSharedComputation
import io.novafoundation.nova.feature_account_impl.data.fee.types.hydra.RealHydrationFeeInjector
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.network.updaters.BlockNumberUpdater
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

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
        blockNumberUpdater: BlockNumberUpdater
    ): HydraDxQuoteSharedComputation {
        return HydraDxQuoteSharedComputation(
            computationalCache = computationalCache,
            quotingFactory = quotingFactory,
            pathQuoterFactory = pathQuoterFactory,
            storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory,
            blockNumberUpdater = blockNumberUpdater
        )
    }

    @Provides
    @FeatureScope
    fun provideAssetHubFeePaymentProvider(
        chainRegistry: ChainRegistry,
        multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        multiLocationConverterFactory: MultiLocationConverterFactory,
    ) = AssetHubFeePaymentProvider(
        chainRegistry,
        multiChainRuntimeCallsApi,
        remoteStorageSource,
        multiLocationConverterFactory
    )

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
        accountRepository: AccountRepository
    ) = HydrationFeePaymentProvider(
        chainRegistry,
        hydraDxQuoteSharedComputation,
        hydrationFeeInjector,
        accountRepository
    )

    @Provides
    @FeatureScope
    fun provideDefaultFeePaymentProvider() = DefaultFeePaymentProvider()

    @Provides
    @FeatureScope
    fun provideFeePaymentProviderRegistry(
        defaultFeePaymentProvider: DefaultFeePaymentProvider,
        assetHubFeePaymentProvider: AssetHubFeePaymentProvider,
        hydrationFeePaymentProvider: HydrationFeePaymentProvider
    ): FeePaymentProviderRegistry = RealFeePaymentProviderRegistry(
        defaultFeePaymentProvider,
        assetHubFeePaymentProvider,
        hydrationFeePaymentProvider
    )
}
