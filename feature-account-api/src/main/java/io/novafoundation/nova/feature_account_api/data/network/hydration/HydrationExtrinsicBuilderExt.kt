package io.novafoundation.nova.feature_account_api.data.network.hydration

import io.novafoundation.nova.common.utils.Modules
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

fun ExtrinsicBuilder.setFeeCurrency(onChainId: HydraDxAssetId) {
    call(
        moduleName = Modules.MULTI_TRANSACTION_PAYMENT,
        callName = "set_currency",
        arguments = mapOf(
            "currency" to onChainId
        )
    )
}
