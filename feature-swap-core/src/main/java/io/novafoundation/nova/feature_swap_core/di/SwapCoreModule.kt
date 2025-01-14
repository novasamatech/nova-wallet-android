package io.novafoundation.nova.feature_swap_core.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.RealHydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.di.conversions.HydraDxConversionModule
import io.novafoundation.nova.feature_swap_core.domain.paths.RealPathQuoterFactory
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [HydraDxConversionModule::class])
class SwapCoreModule {

    @Provides
    @FeatureScope
    fun provideHydraDxAssetIdConverter(
        chainRegistry: ChainRegistry
    ): HydraDxAssetIdConverter {
        return RealHydraDxAssetIdConverter(chainRegistry)
    }

    @Provides
    @FeatureScope
    fun providePathsQuoterFactory(
        computationalCache: ComputationalCache
    ): PathQuoter.Factory {
        return RealPathQuoterFactory(computationalCache)
    }
}
