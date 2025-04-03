package io.novafoundation.nova.feature_assets.presentation.trade.webInterface

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.trade.common.TradeProviderFlowType
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class TradeWebPayload(
    val asset: AssetPayload,
    val providerId: String,
    val type: TradeProviderFlowType
) : Parcelable
