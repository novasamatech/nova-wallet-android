package io.novafoundation.nova.common.data.network.ext

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import java.math.BigInteger


fun AccountInfo.transferableBalance(): BigInteger {
    return transferableMode.calculateTransferable(data)
}

val AccountInfo.transferableMode: TransferableMode
    get() = if (data.flags.holdsAndFreezesEnabled()) {
        TransferableMode.HOLDS_AND_FREEZES
    } else {
        TransferableMode.REGULAR
    }
