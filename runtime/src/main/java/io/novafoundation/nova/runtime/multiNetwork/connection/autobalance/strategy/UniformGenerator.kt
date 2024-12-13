package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.common.utils.cycle
import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

class UniformGenerator(
    private val availabelNodes: List<NodeWithSaturatedUrl>,
) : NodeSequenceGenerator {

    override fun generateNodeSequence(): Sequence<NodeWithSaturatedUrl> {
        return availabelNodes.shuffled().cycle()
    }
}
