package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.NodeSelectionStrategyProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class NodeAutobalancer(
    private val autobalanceStrategyProvider: NodeSelectionStrategyProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun connectionUrlFlow(
        chainId: ChainId,
        changeConnectionEventFlow: Flow<Unit>,
        availableNodesFlow: Flow<Chain.Nodes>,
    ): Flow<NodeWithSaturatedUrl?> {
        return availableNodesFlow.transformLatest { nodesConfig ->
            Log.d(this@NodeAutobalancer.LOG_TAG, "Using ${nodesConfig.wssNodeSelectionStrategy} strategy for switching nodes in $chainId")

            val strategy = autobalanceStrategyProvider.createWss(nodesConfig)

            val nodeIterator = strategy.generateNodeSequence().iterator()
            if (!nodeIterator.hasNext()) {
                Log.w(this@NodeAutobalancer.LOG_TAG, "No wss nodes available for chain $chainId using strategy $strategy")
                return@transformLatest
            }

            emit(nodeIterator.next())

            val updates = changeConnectionEventFlow.map { nodeIterator.next() }
            emitAll(updates)
        }
    }
}
