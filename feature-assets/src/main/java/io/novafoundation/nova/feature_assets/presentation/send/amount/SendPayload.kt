package io.novafoundation.nova.feature_assets.presentation.send.amount

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.android.parcel.Parcelize

sealed class SendPayload : Parcelable {

    @Parcelize
    class SpecifiedOrigin(val origin: AssetPayload) : SendPayload()

    @Parcelize
    class SpecifiedDestination(val destination: AssetPayload) : SendPayload()
}
