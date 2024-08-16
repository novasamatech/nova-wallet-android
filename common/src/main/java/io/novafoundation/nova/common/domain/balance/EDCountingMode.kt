package io.novafoundation.nova.common.domain.balance

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
