package io.novafoundation.nova.feature_governance_api.domain.locks.reusable

import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlin.time.Duration

class LocksChange(
    val lockedAmountChange: Change<Balance>,
    val lockedPeriodChange: Change<Duration>,
    val transferableChange: Change<Balance>
)

class ReusableLock(val type: Type, val amount: Balance) {
    enum class Type {
        GOVERNANCE, ALL
    }
}

fun MutableList<ReusableLock>.addIfPositive(type: ReusableLock.Type, amount: Balance) {
    if (amount.isPositive()) {
        add(ReusableLock(type, amount))
    }
}
