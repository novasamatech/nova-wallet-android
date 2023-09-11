package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.core.updater.Updater

interface StakingUpdater<V> : Updater<V> {

    override val requiredModules: List<String>
        get() = listOf(Modules.STAKING)
}
