package io.novafoundation.nova.feature_gift_impl.presentation.amount

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectGiftAmountPayload(val assetPayload: AssetPayload) : Parcelable
