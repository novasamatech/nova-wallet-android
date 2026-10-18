package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call

class BittensorRootClaimOperation(val hotkey: AccountIdKey) {
    init {
        require(hotkey.value.size == 32)
    }

    fun build(builder: ExtrinsicBuilder) {
        builder.call("SubtensorModule", "claim_root_with_hotkey", mapOf("hotkey" to hotkey.value))
    }
}
