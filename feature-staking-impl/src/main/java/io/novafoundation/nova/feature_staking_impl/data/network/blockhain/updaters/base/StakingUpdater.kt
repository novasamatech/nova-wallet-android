package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface StakingUpdater<V> : Updater<V> {

    /**
     * Can be used to override chainId to launch updater on in case desired chain id
     * is different from staking chain itself
     */
    fun getSyncChainId(stakingChain: Chain): ChainId {
        return stakingChain.id
    }
}

class DelegateToTimeLineChainUpdater<T>(private val delegate: Updater<T>): Updater<T> by delegate, StakingUpdater<T> {

    override fun getSyncChainId(stakingChain: Chain): ChainId {
        return stakingChain.timelineChainIdOrSelf()
    }
}

class AsStakingUpdater<T>(private val delegate: Updater<T>): Updater<T> by delegate, StakingUpdater<T>
