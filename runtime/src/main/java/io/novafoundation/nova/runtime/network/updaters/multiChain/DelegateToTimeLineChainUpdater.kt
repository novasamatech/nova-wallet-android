package io.novafoundation.nova.runtime.network.updaters.multiChain

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class DelegateToTimeLineChainUpdater<T>(private val delegate: Updater<T>) : Updater<T> by delegate, SharedStateBasedUpdater<T> {

    override fun getSyncChainId(sharedStateChain: Chain): ChainId {
        return sharedStateChain.timelineChainIdOrSelf()
    }
}
