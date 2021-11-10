package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RoundRobinStrategy : AutoBalanceStrategy {

    override fun nextNode(currentNode: Chain.Node, defaultNodes: List<Chain.Node>): Chain.Node {
        val indexOfCurrent = defaultNodes.indexOf(currentNode)

        val indexOfNext = if (indexOfCurrent == -1) 0 else (indexOfCurrent + 1) % defaultNodes.size

        return defaultNodes[indexOfNext]
    }
}
