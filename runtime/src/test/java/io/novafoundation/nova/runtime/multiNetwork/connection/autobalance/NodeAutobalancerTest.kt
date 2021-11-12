package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.RoundRobinStrategy
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import jp.co.soramitsu.fearless_utils.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NodeAutobalancerTest {

    @Mock
    lateinit var strategyProvider: AutoBalanceStrategyProvider

    private val autobalancer: NodeAutobalancer = NodeAutobalancer(strategyProvider)

    private val nodes = generateNodes()

    private val nodesFlow = MutableStateFlow(nodes)
    private val stateFlow = MutableStateFlow(SocketStateMachine.State.Disconnected)

    @Test
    fun setup() {
        whenever(strategyProvider.strategyFlowFor(any())).thenReturn(flowOf(RoundRobinStrategy()))
    }

    @Test(timeout = 100)
    fun shouldSelectInitialNode() = runBlocking {
        val nodeFlow = nodeFlow(this)

        val initial = nodeFlow.first()

        assertEquals(nodes.first(), initial)
    }

    private fun generateNodes() = (1..10).map {
        Chain.Node(url = it.toString(), name = it.toString())
    }

    private fun nodeFlow(scope: CoroutineScope) = autobalancer.balancingNodeFlow(
        chainId = "test",
        socketStateFlow = stateFlow,
        availableNodesFlow = nodesFlow,
        scope = scope
    )
}
