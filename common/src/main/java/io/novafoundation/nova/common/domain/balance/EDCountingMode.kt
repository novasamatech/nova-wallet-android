package io.novafoundation.nova.common.domain.balance

import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

enum class EDCountingMode {
    TOTAL, FREE
}

fun EDCountingMode.calculateBalanceCountedTowardsEd(free: BigInteger, reserved: BigInteger): BigInteger {
    return when (this) {
        EDCountingMode.TOTAL -> totalBalance(free, reserved)
        EDCountingMode.FREE -> free
    }
}

fun EDCountingMode.reservedPreventsDusting(reserved: BigInteger): Boolean {
    return when(this) {
        EDCountingMode.TOTAL -> false
        EDCountingMode.FREE -> reserved.isPositive()
    }
}
