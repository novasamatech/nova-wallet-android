package io.novafoundation.nova.feature_assets.presentation.tradeProvider

import androidx.annotation.DrawableRes


data class TradeProviderRvItem(
    val id: String,
    val providerLogoRes: Int,
    val paymentMethods: List<PaymentMethod>,
    val description: String
) {
    sealed interface PaymentMethod {
        class ByResId(@DrawableRes val resId: Int) : PaymentMethod

        class ByText(val text: String) : PaymentMethod
    }
}
