package io.novafoundation.nova.feature_gift_impl.presentation.gifts

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.parcelize.Parcelize

sealed interface GiftsPayload : Parcelable {

    @Parcelize
    object AllAssets : GiftsPayload

    @Parcelize
    data class ByAsset(val assetPayload: AssetPayload) : GiftsPayload
}
