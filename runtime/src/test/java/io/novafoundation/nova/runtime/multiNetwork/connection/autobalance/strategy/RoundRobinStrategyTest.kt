package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.lang.IndexOutOfBoundsException
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundRobinStrategyTest {

    private val strategy = RoundRobinStrategy()

    @Test(expected = IndexOutOfBoundsException::class)
    fun `should select first node if current is not present in available`() = runTest(
        current = createFakeNode("3"),
        all = listOf(
            createFakeNode("1"),
            createFakeNode("2")
        ),
        expected = null
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
        expected: Chain.Node?,
    ) {
        val nodeSequence = strategy.generateNodeSequence(all)
        val indexOfCurrent = all.indexOf(current)
        if (indexOfCurrent == -1) throw IndexOutOfBoundsException()

        val nextItem = nodeSequence.elementAt(indexOfCurrent + 1)

        assertEquals(expected, nextItem)
    }

    private fun createFakeNode(id: String) = Chain.Node(unformattedUrl = id, name = id, chainId = "test", orderId = 0)
}
