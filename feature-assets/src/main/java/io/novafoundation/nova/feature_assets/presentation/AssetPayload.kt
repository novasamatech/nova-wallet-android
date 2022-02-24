package io.novafoundation.nova.feature_assets.presentation

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class AssetPayload(val chainId: ChainId, val chainAssetId: Int) : Parcelable
