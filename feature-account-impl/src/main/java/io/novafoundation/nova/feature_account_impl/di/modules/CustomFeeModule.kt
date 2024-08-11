package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.RealFeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_impl.data.fee.chains.AssetHubFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.DefaultFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.chains.HydrationFeePaymentProvider
import io.novafoundation.nova.feature_account_impl.data.fee.utils.HydraDxQuoteSharedComputation
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxAssetConversionFactory
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class CustomFeeModule {

    @Provides
    @FeatureScope
    fun provideHydraDxQuoteSharedComputation(
        computationalCache: ComputationalCache,
        assetConversionFactory: HydraDxAssetConversionFactory
    ): HydraDxQuoteSharedComputation {
        return HydraDxQuoteSharedComputation(
            computationalCache,
            assetConversionFactory
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
    fun provideHydrationFeePaymentProvider(
        chainRegistry: ChainRegistry,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
        hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation
    ) = HydrationFeePaymentProvider(
        chainRegistry,
        hydraDxAssetIdConverter,
        hydraDxQuoteSharedComputation
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
