package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.feature_wallet_api.domain.model.calculateTotalBalance
import java.math.BigInteger

val AccountInfo.totalBalance: BigInteger
    get() = calculateTotalBalance(
        freeInPlanks = data.free,
        reservedInPlanks = data.reserved
    )
