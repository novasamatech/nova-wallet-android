package io.novafoundation.nova.feature_nft_impl.presentation

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class NftPayload(val chainId: ChainId, val identifier: String) : Parcelable
