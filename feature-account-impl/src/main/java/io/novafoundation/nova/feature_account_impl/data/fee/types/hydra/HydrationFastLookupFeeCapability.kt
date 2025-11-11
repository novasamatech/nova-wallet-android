package io.novafoundation.nova.feature_account_impl.data.fee.types.hydra

import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

class HydrationFastLookupFeeCapability(
    override val nonUtilityFeeCapableTokens: Set<ChainAssetId>
) : FastLookupCustomFeeCapability
