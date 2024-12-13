package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool

import io.novafoundation.nova.common.utils.padEnd
import io.novasama.substrate_sdk_android.runtime.AccountId

fun omniPoolAccountId(): AccountId {
    return "modlomnipool".encodeToByteArray().padEnd(expectedSize = 32, padding = 0)
}
