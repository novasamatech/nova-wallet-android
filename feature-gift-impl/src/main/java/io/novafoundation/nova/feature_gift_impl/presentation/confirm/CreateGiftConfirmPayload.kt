package io.novafoundation.nova.feature_gift_impl.presentation.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import java.math.BigDecimal
import kotlinx.parcelize.Parcelize

@Parcelize
class CreateGiftConfirmPayload(
    val amount: BigDecimal,
    val transferringMaxAmount: Boolean,
    val assetPayload: AssetPayload
) : Parcelable
