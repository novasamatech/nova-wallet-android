package io.novafoundation.nova.feature_governance_api.domain.locks.reusable

import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.absoluteDifference
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
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

fun List<LocksChange>.maximize(): LocksChange {
    val maxLockedAmountChange = this.maxByOrNull { it.lockedAmountChange.absoluteDifference() }
    val maxLockedPeriodChange = this.maxByOrNull { it.lockedPeriodChange.absoluteDifference() }
    val maxTransferableChange = this.maxByOrNull { it.transferableChange.absoluteDifference() }

    return LocksChange(
        maxLockedAmountChange?.lockedAmountChange ?: Change(BigInteger.ZERO, BigInteger.ZERO),
        maxLockedPeriodChange?.lockedPeriodChange ?: Change(Duration.ZERO, Duration.ZERO),
        maxTransferableChange?.transferableChange ?: Change(BigInteger.ZERO, BigInteger.ZERO)
    )
}
