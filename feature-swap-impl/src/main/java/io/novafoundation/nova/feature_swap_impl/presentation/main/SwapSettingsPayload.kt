package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

@Parcelize
class SwapSettingsPayload(val assetPayload: AssetPayload) : Parcelable
