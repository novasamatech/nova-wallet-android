package io.novafoundation.nova.feature_assets.presentation.send

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.presenatation.fee.FeePaymentCurrencyParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class TransferDraft(
    val amount: BigDecimal,
    val origin: AssetPayload,
    val feePaymentCurrency: FeePaymentCurrencyParcel,
    val destination: AssetPayload,
    val recipientAddress: String,
    val openAssetDetailsOnCompletion: Boolean,
) : Parcelable

val TransferDraft.isCrossChain
    get() = origin.chainId != destination.chainId
