package io.novafoundation.nova.feature_swap_impl.di.exchanges

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class AssetConversionExchangeModule {

    @Provides
    @FeatureScope
    fun provideAssetConversionExchangeFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        runtimeCallsApi: MultiChainRuntimeCallsApi,
        multiLocationConverterFactory: MultiLocationConverterFactory,
        chainStateRepository: ChainStateRepository,
        extrinsicServiceFactory: ExtrinsicService.Factory,
    ): AssetConversionExchangeFactory {
        return AssetConversionExchangeFactory(
            chainStateRepository = chainStateRepository,
            remoteStorageSource = remoteStorageSource,
            runtimeCallsApi = runtimeCallsApi,
            extrinsicServiceFactory = extrinsicServiceFactory,
            multiLocationConverterFactory = multiLocationConverterFactory
        )
    }
}
