package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.model

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

@Parcelize
class StartStakingLandingPayload(
    val chainId: ChainId,
    val assetId: Int,
    val stakingTypes: List<Chain.Asset.StakingType>
) : Parcelable
