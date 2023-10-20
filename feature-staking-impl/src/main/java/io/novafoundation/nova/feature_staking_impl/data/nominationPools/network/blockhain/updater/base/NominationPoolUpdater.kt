package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.base

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.updater.Updater

interface NominationPoolUpdater<V> : Updater<V> {

    override val requiredModules: List<String>
        get() = listOf(Modules.STAKING, Modules.NOMINATION_POOLS)
}
