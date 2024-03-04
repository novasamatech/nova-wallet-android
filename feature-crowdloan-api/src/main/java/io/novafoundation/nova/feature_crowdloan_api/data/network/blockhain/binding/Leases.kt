package io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novasama.substrate_sdk_android.runtime.AccountId

class LeaseEntry(
    val accountId: AccountId,
    val locked: BalanceOf
)

fun bindLeases(decoded: Any?): List<LeaseEntry?> {
    return bindList(decoded) {
        it?.let {
            val (accountIdRaw, balanceRaw) = it.cast<List<*>>()

            LeaseEntry(
                accountId = bindAccountId(accountIdRaw),
                locked = bindNumber(balanceRaw)
            )
        }
    }
}
