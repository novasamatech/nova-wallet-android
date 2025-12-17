package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability

class AssetHubFastLookupFeeCapability(
    override val nonUtilityFeeCapableTokens: Set<Int>,
) : FastLookupCustomFeeCapability
