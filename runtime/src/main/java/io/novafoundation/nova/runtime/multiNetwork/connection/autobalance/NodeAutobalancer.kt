package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class NodeAutobalancer(
    private val autobalanceStrategyProvider: AutoBalanceStrategyProvider,
) {

    fun balancingNodeFlow(
        chainId: ChainId,
        changeConnectionEventFlow: Flow<Unit>,
        availableNodesFlow: Flow<List<Chain.Node>>,
        scope: CoroutineScope,
    ): Flow<Chain.Node> {
        val result = MutableSharedFlow<Chain.Node>(replay = 1)

        combine(
            autobalanceStrategyProvider.strategyFlowFor(chainId),
            availableNodesFlow
        ) { first, second ->
            Pair(first, second)
        }
            .onEach { (strategy, nodes) -> result.emit(strategy.initialNode(nodes)) }
            .flatMapLatest { (strategy, nodes) ->
                changeConnectionEventFlow
                    .map { strategy.nextNode(result.first(), nodes) }
            }.onEach(result::emit)
            .launchIn(scope)

        return result
    }
}
