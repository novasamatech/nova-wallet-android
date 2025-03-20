package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2

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
    ENABLED,

    /**
     * Selected payment currency is dictated by the [Fee] returned by [FeeLoaderMixinV2.Presentation.loadFee]
     * User cannot change that
     */
    DETECT_FROM_FEE
}

/**
 * Whether the current mode allows to switch fee asset automatically, e.g. when mixin detects that there is not enough
 * tokens in current fee token
 */
fun PaymentCurrencySelectionMode.automaticChangeEnabled(): Boolean {
    return when (this) {
        PaymentCurrencySelectionMode.AUTOMATIC_ONLY,
        PaymentCurrencySelectionMode.ENABLED -> true

        PaymentCurrencySelectionMode.DISABLED,
        PaymentCurrencySelectionMode.DETECT_FROM_FEE -> false
    }
}

/**
 * Whether the current mode allows user to switch fee asset manually
 */
fun PaymentCurrencySelectionMode.userCanChangeFee(): Boolean {
    return when (this) {
        PaymentCurrencySelectionMode.ENABLED -> true

        PaymentCurrencySelectionMode.AUTOMATIC_ONLY,
        PaymentCurrencySelectionMode.DISABLED,
        PaymentCurrencySelectionMode.DETECT_FROM_FEE -> false
    }
}

/**
 * Whether only native fee is allowed
 */
fun PaymentCurrencySelectionMode.onlyNativeFeeEnabled(): Boolean {
    return when (this) {
        PaymentCurrencySelectionMode.DISABLED -> true

        PaymentCurrencySelectionMode.AUTOMATIC_ONLY,
        PaymentCurrencySelectionMode.ENABLED,
        PaymentCurrencySelectionMode.DETECT_FROM_FEE -> false
    }
}

fun PaymentCurrencySelectionMode.shouldDetectFeeAssetFromFee(): Boolean {
    return when (this) {
        PaymentCurrencySelectionMode.DETECT_FROM_FEE -> true

        PaymentCurrencySelectionMode.AUTOMATIC_ONLY,
        PaymentCurrencySelectionMode.ENABLED,
        PaymentCurrencySelectionMode.DISABLED -> false
    }
}
