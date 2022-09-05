package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundRobinStrategyTest {

    private val strategy = RoundRobinStrategy()

    @Test
    fun `should select first node if current is not present in available`() = runTest(
        current = createFakeNode("3"),
        all = listOf(
            createFakeNode("1"),
            createFakeNode("2")
        ),
        expected = createFakeNode("1")
    )

    @Test
    fun `should select first node after last`() = runTest(
        current = createFakeNode("3"),
        all = listOf(
            createFakeNode("1"),
            createFakeNode("2"),
            createFakeNode("3")
        ),
        expected = createFakeNode("1")
    )

    @Test
    fun `should select next node if not last`() = runTest(
        current = createFakeNode("2"),
        all = listOf(
            createFakeNode("1"),
            createFakeNode("2"),
            createFakeNode("3")
        ),
        expected = createFakeNode("3")
    )

    private fun runTest(
        current: Chain.Node,
        all: List<Chain.Node>,
        expected: Chain.Node,
    ) {
        val nextActual = strategy.nextNode(current, all)

        assertEquals(expected, nextActual)
    }

    private fun createFakeNode(id: String) = Chain.Node(url = id, name = id, chainId = "test", orderId = 0)
}
