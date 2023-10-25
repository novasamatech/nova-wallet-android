package io.novafoundation.nova.feature_assets.presentation.send

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class TransferDraft(
    val amount: BigDecimal,
    val originFee: FeeParcelModel,
    val crossChainFee: BigDecimal?,
    val origin: AssetPayload,
    val destination: AssetPayload,
    val recipientAddress: String
) : Parcelable

val TransferDraft.isCrossChain
    get() = origin.chainId != destination.chainId
