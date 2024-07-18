package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.NodeSelectionStrategy
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

class NodeSelectionStrategyProvider {

    private val roundRobin = RoundRobinStrategy()
    private val uniform = UniformStrategy()

    fun strategyFlowFor(chainId: ChainId, default: NodeSelectionStrategy): Flow<NodeSelectionSequenceStrategy> {
        return flowOf { strategyFor(default) }
    }

    fun strategyFor(config: NodeSelectionStrategy): NodeSelectionSequenceStrategy {
        return when (config) {
            is NodeSelectionStrategy.AutoBalance -> autobalanceStrategyFor(config)
            is NodeSelectionStrategy.SelectedNode -> SelectedNodeStrategy(config.nodeUrl, autobalanceStrategyFor(config.autoBalanceStrategy))
        }
    }

    private fun autobalanceStrategyFor(config: NodeSelectionStrategy.AutoBalance): NodeSelectionSequenceStrategy {
        return when (config) {
            NodeSelectionStrategy.AutoBalance.ROUND_ROBIN -> roundRobin
            NodeSelectionStrategy.AutoBalance.UNIFORM -> uniform
        }
    }
}
