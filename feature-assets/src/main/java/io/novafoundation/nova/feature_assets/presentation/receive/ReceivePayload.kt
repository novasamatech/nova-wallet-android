package io.novafoundation.nova.feature_assets.presentation.receive

import android.os.Parcelable
import androidx.annotation.StringRes
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

sealed class ReceivePayload: Parcelable {

    @Parcelize
    class Asset(val assetPayload: AssetPayload): ReceivePayload()

    @Parcelize
    class Chain(
        val chainId: ChainId,
        @StringRes val titleRes: Int?
    ): ReceivePayload()
}
