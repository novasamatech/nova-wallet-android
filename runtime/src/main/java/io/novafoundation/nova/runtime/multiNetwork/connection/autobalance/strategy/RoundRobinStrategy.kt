package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RoundRobinStrategy : AutoBalanceStrategy {

    override fun initialNode(defaultNodes: List<Chain.Node>): Chain.Node {
        requireNonEmpty(defaultNodes)

        return defaultNodes.first()
    }

    override fun nextNode(currentNode: Chain.Node, defaultNodes: List<Chain.Node>): Chain.Node {
        requireNonEmpty(defaultNodes)

        val indexOfCurrent = defaultNodes.indexOf(currentNode)

        val indexOfNext = if (indexOfCurrent == -1) 0 else (indexOfCurrent + 1) % defaultNodes.size

        return defaultNodes[indexOfNext]
    }

    private fun requireNonEmpty(nodes: List<Chain.Node>) = require(nodes.isNotEmpty()) {
        "Cannot start connection with no available nodes"
    }
}
