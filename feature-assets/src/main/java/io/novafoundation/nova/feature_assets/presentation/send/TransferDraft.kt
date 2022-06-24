package io.novafoundation.nova.feature_assets.presentation.send

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class TransferDraft(
    val amount: BigDecimal,
    val originFee: BigDecimal,
    val crossChainFee: BigDecimal?,
    val origin: AssetPayload,
    val destination: AssetPayload,
    val recipientAddress: String
) : Parcelable

val TransferDraft.isCrossChain
    get() = origin.chainId != destination.chainId
