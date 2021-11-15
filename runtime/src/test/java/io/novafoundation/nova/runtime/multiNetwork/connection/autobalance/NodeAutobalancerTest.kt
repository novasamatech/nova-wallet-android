package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance

import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.RoundRobinStrategy
import io.novafoundation.nova.test_shared.CoroutineTest
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import jp.co.soramitsu.fearless_utils.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NodeAutobalancerTest : CoroutineTest() {

    @Mock
    lateinit var strategyProvider: AutoBalanceStrategyProvider

    lateinit var autobalancer: NodeAutobalancer

    private val nodes = generateNodes()

    private val nodesFlow = MutableStateFlow(nodes)
    private val stateFlow = MutableStateFlow<SocketStateMachine.State>(SocketStateMachine.State.Disconnected)

    @Before
    fun setup() {
        autobalancer = NodeAutobalancer(strategyProvider)
        whenever(strategyProvider.strategyFlowFor(any())).thenReturn(flowOf(RoundRobinStrategy()))
    }

    @Test
    fun shouldSelectInitialNode() = runCoroutineTest {
        val nodeFlow = nodeFlow(this)

        val initial = nodeFlow.first()

        assertEquals(nodes.first(), initial)
    }

    @Test
    fun shouldSelectNodeOnReconnectState() = runCoroutineTest {
        val nodeFlow = nodeFlow(this)
        stateFlow.emit(triggerState(attempt = 4))

        assertEquals(nodes.second(), nodeFlow.first())
    }

    @Test
    fun shouldNotAutobalanceIfNotEnoughAttempts() = runCoroutineTest {
        val nodeFlow = nodeFlow(this)
        stateFlow.emit(triggerState(attempt = 3))

        assertEquals(nodes.first(), nodeFlow.first())
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

    private fun triggerState(attempt: Int) = SocketStateMachine.State.WaitingForReconnect(
        url = "test",
        attempt = attempt,
        pendingSendables = emptySet()
    )
}
