package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.NodeSelectionStrategy
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

class AutoBalanceStrategyProvider {

    private val roundRobin = RoundRobinStrategy()
    private val uniform = UniformStrategy()

    fun strategyFlowFor(chainId: ChainId, default: NodeSelectionStrategy): Flow<AutoBalanceStrategy> {
        return flowOf { strategyFor(default) }
    }

    private fun strategyFor(config: NodeSelectionStrategy): AutoBalanceStrategy {
        return when (config) {
            NodeSelectionStrategy.ROUND_ROBIN -> roundRobin
            NodeSelectionStrategy.UNIFORM -> uniform
        }
    }
}
