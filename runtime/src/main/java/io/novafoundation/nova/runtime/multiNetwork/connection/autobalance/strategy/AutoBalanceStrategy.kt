package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AutoBalanceStrategy {

    fun generateNodeSequence(defaultNodes: List<Chain.Node>): Sequence<Chain.Node>
}
