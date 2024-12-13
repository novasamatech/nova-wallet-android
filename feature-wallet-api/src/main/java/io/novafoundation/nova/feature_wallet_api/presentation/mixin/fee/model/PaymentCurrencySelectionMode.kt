package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency

enum class PaymentCurrencySelectionMode {

    /**
     * Payment currency cannot be changed and is always equal to [FeePaymentCurrency.Native]
     */
    DISABLED,

    /**
     * Payment currency can be changed by automatic internal logic, e.g. when there is not enough balance
     */
    AUTOMATIC_ONLY,

    /**
     * Payment currency can be changed both by automatic internal logic and the user
     */
    ENABLED
}

fun PaymentCurrencySelectionMode.automaticChangeEnabled(): Boolean {
    return this != PaymentCurrencySelectionMode.DISABLED
}

fun PaymentCurrencySelectionMode.userCanChangeFee(): Boolean {
    return this == PaymentCurrencySelectionMode.ENABLED
}

fun PaymentCurrencySelectionMode.onlyNativeFeeEnabled(): Boolean {
    return this == PaymentCurrencySelectionMode.DISABLED
}
