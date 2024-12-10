package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MultiStakingOptionIds
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

@Parcelize
class AvailableStakingOptionsPayload(
    val chainId: ChainId,
    val assetId: Int,
    val stakingTypes: List<Chain.Asset.StakingType>
) : Parcelable

fun AvailableStakingOptionsPayload.toStakingOptionIds(): MultiStakingOptionIds {
    return MultiStakingOptionIds(
        chainId = chainId,
        chainAssetId = assetId,
        stakingTypes = stakingTypes
    )
}
