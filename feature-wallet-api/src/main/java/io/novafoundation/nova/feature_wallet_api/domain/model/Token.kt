package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.planksFromAmount
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

interface TokenBase {

    val currency: Currency

    val coinRate: CoinRate?

    val configuration: Chain.Asset

    fun amountToFiat(tokenAmount: BigDecimal): BigDecimal = toFiatOrNull(tokenAmount).orZero()

    fun planksToFiat(tokenAmountPlanks: BigInteger): BigDecimal = planksToFiatOrNull(tokenAmountPlanks).orZero()
}

data class Token(
    override val currency: Currency,
    override val coinRate: CoinRateChange?,
    override val configuration: Chain.Asset
): TokenBase {
    // TODO move out of the class when Context Receivers will be stable
    fun BigDecimal.toPlanks() = planksFromAmount(this)
    fun BigInteger.toAmount() = amountFromPlanks(this)
}

data class HistoricalToken(
    override val currency: Currency,
    override val coinRate: HistoricalCoinRate?,
    override val configuration: Chain.Asset
): TokenBase

fun TokenBase.toFiatOrNull(tokenAmount: BigDecimal): BigDecimal? = coinRate?.convertAmount(tokenAmount)

fun TokenBase.planksToFiatOrNull(tokenAmountPlanks: BigInteger): BigDecimal? = coinRate?.convertPlanks(configuration, tokenAmountPlanks)

fun TokenBase.amountFromPlanks(amountInPlanks: BigInteger) = configuration.amountFromPlanks(amountInPlanks)

fun TokenBase.planksFromAmount(amount: BigDecimal): BigInteger = configuration.planksFromAmount(amount)

fun Chain.Asset.amountFromPlanks(amountInPlanks: BigInteger) = amountInPlanks.amountFromPlanks(precision)

fun Chain.Asset.planksFromAmount(amount: BigDecimal): BigInteger = amount.planksFromAmount(precision)
