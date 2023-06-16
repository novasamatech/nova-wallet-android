package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model

import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigDecimal

interface PoolBalanceConvertable {

    val poolPoints: PoolPoints

    val poolBalance: Balance
}

val PoolBalanceConvertable.balanceToPointsRatio: BigDecimal
    get() = poolBalance.divideToDecimal(poolPoints.value)

fun PoolBalanceConvertable.amountOf(memberPoints: PoolPoints): Balance {
    return (balanceToPointsRatio * memberPoints.value.toBigDecimal()).toBigInteger()
}
