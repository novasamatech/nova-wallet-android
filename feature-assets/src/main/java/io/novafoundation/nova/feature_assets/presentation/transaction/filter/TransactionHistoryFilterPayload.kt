package io.novafoundation.nova.feature_assets.presentation.transaction.filter

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.parcelize.Parcelize

@Parcelize
class TransactionHistoryFilterPayload(
    val assetPayload: AssetPayload,
) : Parcelable
