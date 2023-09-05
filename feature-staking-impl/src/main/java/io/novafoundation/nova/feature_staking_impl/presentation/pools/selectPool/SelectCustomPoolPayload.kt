package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class SelectCustomPoolPayload(
    val chainId: ChainId,
    val assetId: Int,
    val stakingType: Chain.Asset.StakingType
) : Parcelable
