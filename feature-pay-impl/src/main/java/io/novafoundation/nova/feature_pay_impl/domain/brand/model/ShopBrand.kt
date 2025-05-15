package io.novafoundation.nova.feature_pay_impl.domain.brand.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Identifiable
import java.math.BigDecimal

class ShopBrand(
    val primaryMetadata: ShopBrandPrimaryMetadata,
    val description: String,
    val terms: String,
    val cashback: Fraction,
    val id: String,
    val paymentLimits: PaymentLimits
) : Identifiable {

    val iconUrl: String
        get() = primaryMetadata.iconUrl

    val name: String
        get() = primaryMetadata.name

    override val identifier: String = id

    class PaymentLimits(
        val minimumFiat: BigDecimal,
        val maximumFiat: BigDecimal
    )

    fun isAmountTooLow(fiatAmount: BigDecimal): Boolean {
        return paymentLimits.minimumFiat > fiatAmount
    }

    fun isAmountTooHigh(fiatAmount: BigDecimal): Boolean {
        return paymentLimits.maximumFiat < fiatAmount
    }

    fun amountInBounds(fiatAmount: BigDecimal): Boolean {
        return fiatAmount in paymentLimits.minimumFiat..paymentLimits.maximumFiat
    }
}
