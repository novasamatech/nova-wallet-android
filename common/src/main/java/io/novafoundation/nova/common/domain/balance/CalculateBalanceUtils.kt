package io.novafoundation.nova.common.domain.balance

import io.novafoundation.nova.common.utils.atLeastZero
import java.math.BigInteger

fun legacyTransferable(free: BigInteger, frozen: BigInteger): BigInteger {
    return (free - frozen).atLeastZero()
}

fun holdAndFreezesTransferable(free: BigInteger, frozen: BigInteger, reserved: BigInteger): BigInteger {
    val freeCannotDropBelow = (frozen - reserved).atLeastZero()

    return (free - freeCannotDropBelow).atLeastZero()
}

fun totalBalance(free: BigInteger, reserved: BigInteger): BigInteger {
    return free + reserved
}
