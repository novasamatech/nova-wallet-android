package io.novafoundation.nova.feature_assets.presentation.trade.provider

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.trade.common.TradeProviderFlowType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class TradeProviderListPayload(
    val chainId: ChainId,
    val assetId: Int,
    val type: TradeProviderFlowType
) : Parcelable {
}
