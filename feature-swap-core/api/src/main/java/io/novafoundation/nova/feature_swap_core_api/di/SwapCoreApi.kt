package io.novafoundation.nova.feature_swap_core_api.di

import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.paths.PathQuoter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback

interface SwapCoreApi {

    val hydraDxQuotingFactory: HydraDxQuoting.Factory

    val hydraDxAssetIdConverter: HydraDxAssetIdConverter

    val hydrationPriceConversionFallback: HydrationPriceConversionFallback

    val pathQuoterFactory: PathQuoter.Factory
}
