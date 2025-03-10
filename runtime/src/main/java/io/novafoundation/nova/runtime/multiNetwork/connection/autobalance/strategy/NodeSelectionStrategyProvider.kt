package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.ext.httpNodes
import io.novafoundation.nova.runtime.ext.wssNodes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.NodeSelectionStrategy
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrl
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrls

class NodeSelectionStrategyProvider(
    private val connectionSecrets: ConnectionSecrets,
) {

    fun createWss(config: Chain.Nodes): NodeSequenceGenerator {
        return createNodeSequenceGenerator(
            availableNodes = config.wssNodes(),
            autobalanceStrategy = config.autoBalanceStrategy,
            nodeSelectionStrategy = config.wssNodeSelectionStrategy
        )
    }

    fun createHttp(config: Chain.Nodes): NodeSequenceGenerator {
        return createNodeSequenceGenerator(
            availableNodes = config.httpNodes(),
            autobalanceStrategy = config.autoBalanceStrategy,
            // Http nodes disregard selected wss strategy and always use auto balance
            nodeSelectionStrategy = NodeSelectionStrategy.AutoBalance
        )
    }

    private fun createNodeSequenceGenerator(
        availableNodes: List<Chain.Node>,
        autobalanceStrategy: Chain.Nodes.AutoBalanceStrategy,
        nodeSelectionStrategy: NodeSelectionStrategy,
    ): NodeSequenceGenerator {
        return when (nodeSelectionStrategy) {
            NodeSelectionStrategy.AutoBalance -> createAutoBalanceGenerator(autobalanceStrategy, availableNodes)
            is NodeSelectionStrategy.SelectedNode -> {
                createSelectedNodeGenerator(nodeSelectionStrategy.unformattedNodeUrl, availableNodes)
                    // Fallback to auto balance in case we failed to setup a selected node strategy
                    ?: createAutoBalanceGenerator(autobalanceStrategy, availableNodes)
            }
        }
    }

    private fun createSelectedNodeGenerator(
        selectedUnformattedNodeUrl: String,
        availableNodes: List<Chain.Node>,
    ): SelectedNodeGenerator? {
        val node = availableNodes.find { it.unformattedUrl == selectedUnformattedNodeUrl } ?: return null
        val saturatedNode = node.saturateNodeUrl(connectionSecrets) ?: return null
        return SelectedNodeGenerator(saturatedNode)
    }

    private fun createAutoBalanceGenerator(
        autoBalanceStrategy: Chain.Nodes.AutoBalanceStrategy,
        availableNodes: List<Chain.Node>,
    ): NodeSequenceGenerator {
        val saturatedUrls = availableNodes.saturateNodeUrls(connectionSecrets)

        return when (autoBalanceStrategy) {
            Chain.Nodes.AutoBalanceStrategy.ROUND_ROBIN -> RoundRobinGenerator(saturatedUrls)
            Chain.Nodes.AutoBalanceStrategy.UNIFORM -> UniformGenerator(saturatedUrls)
        }
    }
}
