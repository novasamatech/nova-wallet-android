package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.test_shared.assertListEquals
import io.novafoundation.nova.test_shared.assertMapEquals
import io.novafoundation.nova.test_shared.assertSetEquals
import org.junit.Test

internal class GraphKtTest {

    @Test
    fun shouldFindConnectedComponents() {
        // 3 and 2 are connected through 1
        testConnectedComponents(
            1 to listOf(2, 3),
            2 to listOf(1),
            3 to listOf(1),
            expectedComponents = listOf(listOf(1, 2, 3))
        )

        testConnectedComponents(
            1 to listOf(2),
            2 to listOf(1),
            3 to emptyList(),
            expectedComponents = listOf(listOf(1, 2), listOf(3))
        )

        testConnectedComponents(
            1 to listOf(2, 3),
            2 to listOf(1),
            3 to listOf(1),
            4 to listOf(5),
            5 to listOf(4),
            6 to emptyList(),
            expectedComponents = listOf(listOf(1, 2, 3), listOf(4, 5), listOf(6))
        )
    }

    @Test
    fun shouldFindAllPossibleDirections() {
        val graph = Graph.createSimple(
            1 to listOf(2, 3),
            2 to listOf(1),
            3 to listOf(1),
            4 to listOf(5),
            5 to listOf(4),
            6 to emptyList(),
        )
        val actual = graph.findAllPossibleDirections()
        val expected = mapOf(
            1 to setOf(2, 3),
            2 to setOf(1, 3),
            3 to setOf(1, 2),
            4 to setOf(5),
            5 to setOf(4),
            6 to emptySet()
        )

        assertMapEquals(expected, actual)
    }

    @Test
    fun shouldFindPaths() {
        val graph = Graph.createSimple(
            1 to listOf(2, 3, 4),
            2 to listOf(1, 4, 3),
            3 to listOf(1, 2),
            4 to listOf(1, 2)
        )

        var actual = graph.findDijkstraPathsBetween(2, 3, limit = 3)
        var expected = listOf(
            listOf(SimpleEdge(2, 3)),
            listOf(SimpleEdge(2, 1), SimpleEdge(1, 3)),
            listOf(SimpleEdge(2, 4), SimpleEdge(4, 1), SimpleEdge(1, 3)),
        )
        assertListEquals(expected, actual)

        actual = graph.findDijkstraPathsBetween(2, 3, limit = 1)
        expected = listOf(
            listOf(SimpleEdge(2, 3)),
        )

        assertListEquals(expected, actual)
    }

    private fun testConnectedComponents(
        vararg adjacencyPairs: Pair<Int, List<Int>>,
        expectedComponents: List<ConnectedComponent<Int>>
    ) {
        val graph = Graph.createSimple(*adjacencyPairs)
        val actualComponents = graph.findConnectedComponents().unordered()
        val expectedUnordered = expectedComponents.unordered()

        assertSetEquals(expectedUnordered, actualComponents)
    }

    private fun <N> Iterable<Iterable<N>>.unordered(): Set<Set<N>> {
        return mapToSet { it.toSet() }
    }
}
