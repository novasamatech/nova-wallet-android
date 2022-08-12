package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class BalanceLocks(
    val locks: List<Lock>
) {

    class Lock(
        val id: String,
        val amount: Balance
    )
}

@HelperBinding
fun bindBalanceLocks(dynamicInstance: Any?): BalanceLocks? {
    if (dynamicInstance == null) return null

    return BalanceLocks(
        bindList(dynamicInstance) {
            BalanceLocks.Lock(
                bindString(it.castToStruct()["id"]),
                bindNumber(it.castToStruct()["amount"])
            )
        }
    )
}
