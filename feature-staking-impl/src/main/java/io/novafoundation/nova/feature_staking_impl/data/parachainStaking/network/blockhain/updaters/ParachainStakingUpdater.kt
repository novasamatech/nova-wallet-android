package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.blockhain.updaters

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.updater.Updater

interface ParachainStakingUpdater<V> : Updater<V> {

    override val requiredModules: List<String>
        get() = listOf(Modules.PARACHAIN_STAKING)
}
