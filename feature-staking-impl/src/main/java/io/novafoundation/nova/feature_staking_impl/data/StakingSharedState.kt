package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState

private const val STAKING_SHARED_STATE = "STAKING_SHARED_STATE"

class StakingSharedState(
    chainRegistry: ChainRegistry,
    preferences: Preferences,
) : SingleAssetSharedState(
    preferences = preferences,
    chainRegistry = chainRegistry,
    filter = { _, chainAsset -> chainAsset.staking != Chain.Asset.StakingType.UNSUPPORTED },
    preferencesKey = STAKING_SHARED_STATE
)
