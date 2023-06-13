package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.planksFromAmount
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

data class Token(
    val rate: BigDecimal?,
    val currency: Currency,
    val recentRateChange: BigDecimal?,
    val configuration: Chain.Asset
) {
    // TODO move out of the class when Context Receivers will be stable
    fun BigDecimal.toPlanks() = planksFromAmount(this)
    fun BigInteger.toAmount() = amountFromPlanks(this)

    fun priceOf(tokenAmount: BigDecimal): BigDecimal = rate?.multiply(tokenAmount) ?: BigDecimal.ZERO
}

fun Token.amountFromPlanks(amountInPlanks: BigInteger) = configuration.amountFromPlanks(amountInPlanks)

fun Token.planksFromAmount(amount: BigDecimal): BigInteger = configuration.planksFromAmount(amount)

fun Chain.Asset.amountFromPlanks(amountInPlanks: BigInteger) = amountInPlanks.amountFromPlanks(precision)

fun Chain.Asset.planksFromAmount(amount: BigDecimal): BigInteger = amount.planksFromAmount(precision)

fun Token.priceOf(planks: Balance) : BigDecimal {
    return priceOf(amountFromPlanks(planks))
}
