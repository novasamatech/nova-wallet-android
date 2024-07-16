package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

class SelectedNodeStrategy(
    private val selectedUrl: String?,
    private val fallbackStrategy: NodeSelectionSequenceStrategy
) : NodeSelectionSequenceStrategy {

    override fun generateNodeSequence(defaultNodes: List<NodeWithSaturatedUrl>): Sequence<NodeWithSaturatedUrl> {
        if (selectedUrl == null) {
            return fallbackStrategy.generateNodeSequence(defaultNodes)
        }

        val selectedNode = defaultNodes.find { it.node.unformattedUrl == selectedUrl }

        return if (selectedNode == null) {
            fallbackStrategy.generateNodeSequence(defaultNodes)
        } else {
            sequenceOf(selectedNode)
        }
    }
}
