package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AutoBalanceStrategy {

    fun generateNodeSequence(defaultNodes: List<Chain.Node>): Sequence<Chain.Node>
}

fun AutoBalanceStrategy.generateNodeIterator(defaultNodes: List<Chain.Node>): Iterator<Chain.Node> {
    return generateNodeSequence(defaultNodes).iterator()
}
