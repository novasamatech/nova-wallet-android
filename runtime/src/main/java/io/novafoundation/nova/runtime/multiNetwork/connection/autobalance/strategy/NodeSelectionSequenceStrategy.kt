package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

interface NodeSelectionSequenceStrategy {

    fun generateNodeSequence(defaultNodes: List<NodeWithSaturatedUrl>): Sequence<NodeWithSaturatedUrl>
}

fun NodeSelectionSequenceStrategy.generateNodeIterator(defaultNodes: List<NodeWithSaturatedUrl>): Iterator<NodeWithSaturatedUrl> {
    return generateNodeSequence(defaultNodes).iterator()
}
