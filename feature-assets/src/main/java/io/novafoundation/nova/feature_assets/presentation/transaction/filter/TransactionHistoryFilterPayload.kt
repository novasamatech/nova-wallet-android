package io.novafoundation.nova.feature_assets.presentation.transaction.filter

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class TransactionHistoryFilterPayload(
    val assetPayload: AssetPayload,
): Parcelable
