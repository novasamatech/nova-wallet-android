package io.novafoundation.nova.feature_swap_core.di

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxAssetConversionFactory
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter

interface SwapCoreApi {

    val hydraDxAssetIdConverter: HydraDxAssetIdConverter

    val hydraDxAssetConversionFactory: HydraDxAssetConversionFactory
}
