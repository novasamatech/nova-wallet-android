package io.novafoundation.nova.feature_pay_impl.data.raise.brands

import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopBrandPrimaryMetadata
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model.RaiseBrandRemote
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model.RaiseBrandResponse
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseAmountConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.convertFromApiCurrency
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopBrand

interface RaiseBrandsConverter {

    fun convertBrandResponseToBrand(response: RaiseBrandResponse): ShopBrand?
}

class RealRaiseBrandsConverter(
    private val raiseAmountConverter: RaiseAmountConverter,
) : RaiseBrandsConverter {

    companion object {

        private const val RAISE_PERCENT_PRECISION = 2
    }

    override fun convertBrandResponseToBrand(response: RaiseBrandResponse): ShopBrand? {
        return ShopBrand(
            id = response.id,
            primaryMetadata = ShopBrandPrimaryMetadata(
                iconUrl = response.attributes.iconUrl,
                name = response.attributes.name,
            ),
            description = response.attributes.description,
            terms = response.attributes.terms,
            cashback = raiseAmountConverter
                .convertFromApiAmount(response.attributes.commissionRate, RAISE_PERCENT_PRECISION)
                .percents,
            paymentLimits = response.attributes.transactionConfig.toPaymentLimits() ?: return null
        )
    }

    private fun RaiseBrandRemote.TransactionConfig.toPaymentLimits(): ShopBrand.PaymentLimits? {
        // TODO we don't supported fixedLoad yet as our amount input component is not suitable for that
        if (variableLoad == null) return null

        return ShopBrand.PaymentLimits(
            minimumFiat = raiseAmountConverter.convertFromApiCurrency(variableLoad.minAmount),
            maximumFiat = raiseAmountConverter.convertFromApiCurrency(variableLoad.maxAmount)
        )
    }
}
