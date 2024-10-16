package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.test_shared.assertListEquals
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class GraphKtTest {

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

    @OptIn(ExperimentalTime::class)
    @Test
    fun testPerformance() {
        val graphSize = 200
        val graph = fullyConnectedGraph(graphSize)

        val time = measureTime {
            repeat(100) { i ->
                graph.findDijkstraPathsBetween(i, graphSize - i, limit = 10)
            }

        }

        print("Execution time: ${time / 100}")
    }

    private fun fullyConnectedGraph(size: Int): SimpleGraph<Int> {
        return Graph.build {
            (0..size).onEach { i ->
                (0..size).onEach { j ->
                    if (i != j) {
                        val edge = SimpleEdge(i, j)
                        addEdge(edge)
                    }
                }
            }
        }
    }
}
