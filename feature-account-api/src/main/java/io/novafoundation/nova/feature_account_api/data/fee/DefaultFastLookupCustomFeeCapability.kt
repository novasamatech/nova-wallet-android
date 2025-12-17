package io.novafoundation.nova.feature_account_api.data.fee

import io.novafoundation.nova.feature_account_api.data.fee.capability.FastLookupCustomFeeCapability
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

class DefaultFastLookupCustomFeeCapability : FastLookupCustomFeeCapability {

    override val nonUtilityFeeCapableTokens: Set<ChainAssetId> = emptySet()
}
