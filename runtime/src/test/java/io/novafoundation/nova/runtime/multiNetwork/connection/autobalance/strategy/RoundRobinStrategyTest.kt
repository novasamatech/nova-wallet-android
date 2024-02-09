package io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundRobinStrategyTest {

    private val strategy = RoundRobinStrategy()

    private val nodes = listOf(
        createFakeNode("1"),
        createFakeNode("2"),
        createFakeNode("3")
    )

    @Test
    fun `collections should have the same sequence`() {
        val iterator = strategy.generateNodeSequence(nodes)
            .iterator()

        nodes.forEach { assertEquals(it, iterator.next()) }
    }

    @Test
    fun `sequence should be looped`() {
        val iterator = strategy.generateNodeSequence(nodes)
            .iterator()

        repeat(nodes.size) { iterator.next() }

        assertEquals(nodes.first(), iterator.next())
    }

    private fun createFakeNode(id: String) = NodeWithSaturatedUrl(
        node = Chain.Node(unformattedUrl = id, name = id, chainId = "test", orderId = 0),
        saturatedUrl = id
    )
}
