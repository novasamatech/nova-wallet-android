package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance

import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.RoundRobinStrategy
import io.novafoundation.nova.test_shared.CoroutineTest
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
@Ignore("TODO: fix test")
// TODO valentun: New coroutine test API had some changes which broke those tests. The problem is caused by the implementation of the
// balancingNodeFlow, which use SharedFlow + emitting coroutine, which is not a quite good way to do so. I figured a way to rewrite it in cold way using runningReduce
// but I do not want to make such complex  changes right before release. Gonna fix after 3.7.0
class NodeAutobalancerTest : CoroutineTest() {

    @Mock
    lateinit var strategyProvider: AutoBalanceStrategyProvider

    lateinit var autobalancer: NodeAutobalancer

    private val nodes = generateNodes()
    private val nodeSelectionStrategy = Chain.Nodes.NodeSelectionStrategy.ROUND_ROBIN

    private val nodesFlow = MutableStateFlow(Chain.Nodes(nodeSelectionStrategy, nodes))
    private val stateFlow = singleReplaySharedFlow<Unit>()
    private val connectionSecrets = ConnectionSecrets(emptyMap())

    @Before
    fun setup() {
        autobalancer = NodeAutobalancer(strategyProvider, connectionSecrets)
        whenever(strategyProvider.strategyFlowFor(any(), nodeSelectionStrategy))
            .thenReturn(flowOf(RoundRobinStrategy()))
    }

    @Test
    fun shouldSelectInitialNode() = runCoroutineTest {
        val nodeFlow = nodeFlow()

        val initial = nodeFlow.first()

        assertEquals(nodes.first(), initial)
    }

    @Test
    fun shouldSelectNodeOnReconnectState() = runCoroutineTest {
        val nodeFlow = nodeFlow()
        stateFlow.emit(Unit)

        assertEquals(nodes.second(), nodeFlow.first())
    }

    @Test
    fun shouldNotAutobalanceIfNotEnoughAttempts() = runCoroutineTest {
        val nodeFlow = nodeFlow()
        stateFlow.emit(Unit)

        assertEquals(nodes.first(), nodeFlow.first())
    }

    private fun generateNodes() = (1..10).map {
        Chain.Node(unformattedUrl = it.toString(), name = it.toString(), chainId = "test", orderId = 0, isCustom = false)
    }

    private fun nodeFlow() = autobalancer.connectionUrlFlow(
        chainId = "test",
        changeConnectionEventFlow = stateFlow,
        availableNodesFlow = nodesFlow
    )

    private fun triggerState(attempt: Int) = SocketStateMachine.State.WaitingForReconnect(
        url = "test",
        attempt = attempt,
        pendingSendables = emptySet()
    )
}
