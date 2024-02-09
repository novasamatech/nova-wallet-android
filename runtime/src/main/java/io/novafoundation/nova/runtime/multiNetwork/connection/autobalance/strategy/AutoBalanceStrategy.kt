package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

interface AutoBalanceStrategy {

    fun generateNodeSequence(defaultNodes: List<NodeWithSaturatedUrl>): Sequence<NodeWithSaturatedUrl>
}

fun AutoBalanceStrategy.generateNodeIterator(defaultNodes: List<NodeWithSaturatedUrl>): Iterator<NodeWithSaturatedUrl> {
    return generateNodeSequence(defaultNodes).iterator()
}
