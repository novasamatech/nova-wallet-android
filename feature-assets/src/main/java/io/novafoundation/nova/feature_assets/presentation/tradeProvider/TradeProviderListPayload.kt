package io.novafoundation.nova.feature_assets.presentation.tradeProvider

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class TradeProviderListPayload(
    val chainId: ChainId,
    val assetId: Int,
    val type: Type
) : Parcelable {
    enum class Type {
        BUY, SELL
    }
}
