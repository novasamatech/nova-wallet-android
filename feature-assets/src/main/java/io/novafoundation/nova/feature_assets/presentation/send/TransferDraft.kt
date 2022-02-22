package io.novafoundation.nova.feature_assets.presentation.send

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class TransferDraft(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val assetPayload: AssetPayload,
    val recipientAddress: String
) : Parcelable {
    @IgnoredOnParcel
    val totalTransaction = amount + fee

    fun totalAfterTransfer(currentTotal: BigDecimal) = currentTotal - totalTransaction
}
