package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class SwapSettingsPayload(val chainId: ChainId, val assetId: ChainAssetId) : Parcelable
