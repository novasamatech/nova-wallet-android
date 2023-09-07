package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model

import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigDecimal
import java.math.RoundingMode

interface PoolBalanceConvertable {

    val poolPoints: PoolPoints

    val poolBalance: Balance
}

val PoolBalanceConvertable.balanceToPointsRatio: BigDecimal
    get() {
        if (poolPoints.value.isZero) return BigDecimal.ZERO

        return poolBalance.divideToDecimal(poolPoints.value)
    }

val PoolBalanceConvertable.pointsToBalanceRatio: BigDecimal
    get() {
        if (poolBalance.isZero) return BigDecimal.ZERO

        return poolPoints.value.divideToDecimal(poolBalance)
    }

fun PoolBalanceConvertable.amountOf(memberPoints: PoolPoints): Balance {
    return (balanceToPointsRatio * memberPoints.value.toBigDecimal()).toBigInteger()
}

fun PoolBalanceConvertable.pointsOf(amount: Balance): PoolPoints {
    val pointsRaw = (pointsToBalanceRatio * amount.toBigDecimal())
        .setScale(0, RoundingMode.UP)
        .toBigInteger()

    return PoolPoints(pointsRaw)
}
