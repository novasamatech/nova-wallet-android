package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

open class CoinRate(val rate: BigDecimal)

class CoinRateChange(val recentRateChange: BigDecimal, value: BigDecimal) : CoinRate(value)

class HistoricalCoinRate(val timestamp: Long, value: BigDecimal) : CoinRate(value)

fun CoinRate.convertAmount(amount: BigDecimal) = amount * rate

fun CoinRate.convertPlanks(asset: Chain.Asset, amount: BigInteger) = convertAmount(asset.amountFromPlanks(amount))
