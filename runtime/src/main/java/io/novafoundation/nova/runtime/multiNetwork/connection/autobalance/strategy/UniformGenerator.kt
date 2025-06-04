package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.common.utils.cycle
import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

class UniformGenerator(
    private val availableNodes: List<NodeWithSaturatedUrl>,
) : NodeSequenceGenerator {

    override fun generateNodeSequence(): Sequence<NodeWithSaturatedUrl> {
        return availableNodes.shuffled().cycle()
    }
}
