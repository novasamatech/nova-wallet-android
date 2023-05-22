package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectableSingleAssetSharedState
import io.novafoundation.nova.runtime.state.NothingAdditional
import io.novafoundation.nova.runtime.state.uniqueOption

private const val STAKING_SHARED_STATE = "STAKING_SHARED_STATE"

class StakingSharedState(
    chainRegistry: ChainRegistry,
    preferences: Preferences,
) : SelectableSingleAssetSharedState<NothingAdditional>(
    preferences = preferences,
    chainRegistry = chainRegistry,
    supportedOptions = uniqueOption { _, chainAsset ->
        // TODO with multi-staking dashboard, we will have separate implementation of SingleAssetSharedState
        val first = chainAsset.staking.firstOrNull() ?: return@uniqueOption false
        first != Chain.Asset.StakingType.UNSUPPORTED
    },
    preferencesKey = STAKING_SHARED_STATE
)
