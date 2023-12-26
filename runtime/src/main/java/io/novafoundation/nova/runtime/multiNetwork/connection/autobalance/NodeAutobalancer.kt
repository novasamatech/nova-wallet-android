package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.runtime.ext.wssNodes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrls
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

class NodeAutobalancer(
    private val autobalanceStrategyProvider: AutoBalanceStrategyProvider,
    private val connectionSecrets: ConnectionSecrets,
) {

    fun connectionUrlFlow(
        chainId: ChainId,
        changeConnectionEventFlow: Flow<Unit>,
        availableNodesFlow: Flow<Chain.Nodes>,
    ): Flow<NodeWithSaturatedUrl?> {
        return availableNodesFlow.flatMapLatest { nodesConfig ->
            autobalanceStrategyProvider.strategyFlowFor(chainId, nodesConfig.nodeSelectionStrategy).transform { strategy ->
                Log.d(this@NodeAutobalancer.LOG_TAG, "Using ${nodesConfig.nodeSelectionStrategy} strategy for switching nodes in $chainId")

                val wssNodes = nodesConfig.wssNodes().saturateNodeUrls(connectionSecrets)

                if (wssNodes.isEmpty()) {
                    Log.w(this@NodeAutobalancer.LOG_TAG, "No wss nodes available for chain $chainId")

                    emit(null)
                }

                val nodeIterator = strategy.generateNodeSequence(wssNodes).iterator()

                emit(nodeIterator.next())

                val updates = changeConnectionEventFlow.map { nodeIterator.next() }
                emitAll(updates)
            }
        }
    }
}
