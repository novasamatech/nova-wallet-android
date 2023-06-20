package io.novafoundation.nova.feature_crowdloan_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.state.SelectableSingleAssetSharedState
import io.novafoundation.nova.runtime.state.NothingAdditional
import io.novafoundation.nova.runtime.state.uniqueOption

private const val CROWDLOAN_SHARED_STATE = "CROWDLOAN_SHARED_STATE"

class CrowdloanSharedState(
    chainRegistry: ChainRegistry,
    preferences: Preferences,
) : SelectableSingleAssetSharedState<NothingAdditional>(
    preferences = preferences,
    chainRegistry = chainRegistry,
    supportedOptions = uniqueOption { chain, chainAsset -> chain.hasCrowdloans and chainAsset.isUtilityAsset },
    preferencesKey = CROWDLOAN_SHARED_STATE
)
