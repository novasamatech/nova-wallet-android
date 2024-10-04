package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

interface NodeSequenceGenerator {

    fun generateNodeSequence(): Sequence<NodeWithSaturatedUrl>
}

fun NodeSequenceGenerator.generateNodeIterator(): Iterator<NodeWithSaturatedUrl> {
    return generateNodeSequence().iterator()
}
