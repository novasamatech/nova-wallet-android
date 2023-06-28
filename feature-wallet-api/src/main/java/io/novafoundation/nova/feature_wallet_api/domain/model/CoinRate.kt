package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

interface CoinRate {
    val rate: BigDecimal
}

class CoinRateChange(val recentRateChange: BigDecimal, override val rate: BigDecimal) : CoinRate

class HistoricalCoinRate(val timestamp: Long, override val rate: BigDecimal) : CoinRate

fun CoinRate.convertAmount(amount: BigDecimal) = amount * rate

fun CoinRate.convertPlanks(asset: Chain.Asset, amount: BigInteger) = convertAmount(asset.amountFromPlanks(amount))

fun List<HistoricalCoinRate>.findNearestCoinRate(timestamp: Long): HistoricalCoinRate? {
    if (isEmpty()) return null
    if (first().timestamp > timestamp) return null // To support the case when the token started trading later than the desired coin rate

    val index = binarySearchFloor { it.timestamp.compareTo(timestamp) }
    return getOrNull(index)
}
