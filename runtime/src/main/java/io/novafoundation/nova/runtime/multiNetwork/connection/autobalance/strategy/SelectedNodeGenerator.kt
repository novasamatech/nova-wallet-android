package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

class SelectedNodeGenerator(
    private val selectedNode: NodeWithSaturatedUrl,
) : NodeSequenceGenerator {

    override fun generateNodeSequence(): Sequence<NodeWithSaturatedUrl> {
        return sequenceOf(selectedNode)
    }
}
