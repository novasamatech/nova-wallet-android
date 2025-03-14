package io.novafoundation.nova.feature_assets.presentation.novacard.topup

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import java.math.BigDecimal
import kotlinx.android.parcel.Parcelize

@Parcelize
class TopUpCardPayload(
    val address: String,
    val amount: BigDecimal,
    val asset: AssetPayload
) : Parcelable
