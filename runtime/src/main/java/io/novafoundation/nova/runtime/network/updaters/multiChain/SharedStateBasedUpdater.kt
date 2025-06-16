package io.novafoundation.nova.runtime.network.updaters.multiChain

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.groupByIntoSet
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface SharedStateBasedUpdater<V> : Updater<V> {

    /**
     * Can be used to override chainId to launch updater on in case desired chain id
     * is different from shared state chain itself
     */
    fun getSyncChainId(sharedStateChain: Chain): ChainId {
        return sharedStateChain.id
    }
}

fun List<SharedStateBasedUpdater<*>>.groupBySyncingChain(sharedStateChain: Chain): MultiMap<ChainId, SharedStateBasedUpdater<*>> {
    return groupByIntoSet(keySelector = { it.getSyncChainId(sharedStateChain) })
}
