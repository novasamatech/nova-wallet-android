package io.novafoundation.nova.common.domain.balance

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import java.math.BigInteger

enum class TransferableMode {
    REGULAR, HOLDS_AND_FREEZES
}

fun TransferableMode.calculateTransferable(free: BigInteger, frozen: BigInteger, reserved: BigInteger): BigInteger {
    return when (this) {
        TransferableMode.REGULAR -> legacyTransferable(free, frozen)
        TransferableMode.HOLDS_AND_FREEZES -> holdAndFreezesTransferable(free, frozen, reserved)
    }
}

fun TransferableMode.calculateTransferable(accountBalance: AccountBalance): BigInteger {
    return calculateTransferable(accountBalance.free, accountBalance.frozen, accountBalance.reserved)
}
