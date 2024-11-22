package io.novafoundation.nova.feature_account_api.presenatation.fee

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.runtime.util.ChainAssetParcel
import kotlinx.android.parcel.Parcelize

sealed class FeePaymentCurrencyParcel : Parcelable {

    @Parcelize
    object Native : FeePaymentCurrencyParcel()

    @Parcelize
    class Asset(val asset: ChainAssetParcel) : FeePaymentCurrencyParcel()
}

fun FeePaymentCurrency.toParcel(): FeePaymentCurrencyParcel {
    return when (this) {
        is FeePaymentCurrency.Asset -> FeePaymentCurrencyParcel.Asset(ChainAssetParcel(asset))
        FeePaymentCurrency.Native -> FeePaymentCurrencyParcel.Native
    }
}

fun FeePaymentCurrencyParcel.toDomain(): FeePaymentCurrency {
    return when (this) {
        is FeePaymentCurrencyParcel.Asset -> FeePaymentCurrency.Asset(asset.value)
        FeePaymentCurrencyParcel.Native -> FeePaymentCurrency.Native
    }
}
