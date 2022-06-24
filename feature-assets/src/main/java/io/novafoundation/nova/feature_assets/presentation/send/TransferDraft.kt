package io.novafoundation.nova.feature_assets.presentation.send

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class TransferDraft(
    val amount: BigDecimal,
    val originFee: BigDecimal,
    val crossChainFee: BigDecimal?,
    val destinationChain: ChainId,
    val assetPayload: AssetPayload,
    val recipientAddress: String
) : Parcelable

val TransferDraft.isCrossChain
    get() = destinationChain != assetPayload.chainId
