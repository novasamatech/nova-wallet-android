package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.common.utils.cycle
import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl

class RoundRobinStrategy : AutoBalanceStrategy {

    override fun generateNodeSequence(defaultNodes: List<NodeWithSaturatedUrl>): Sequence<NodeWithSaturatedUrl> {
        return defaultNodes.cycle()
    }
}
