package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetConversion

interface HydraDxAssetConversionFactory : AssetConversion.Factory<HydraDxSwapEdge>

interface HydraDXAssetConversion : AssetConversion<HydraDxSwapEdge>
