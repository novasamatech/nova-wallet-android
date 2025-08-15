package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.MultiMapList
import java.util.PriorityQueue

interface Edge<N> {

    val from: N

    val to: N
}

interface WeightedEdge<N> : Edge<N> {

    fun weightForAppendingTo(path: Path<WeightedEdge<N>>): Int
}

class Graph<N, E : Edge<N>>(
    val adjacencyList: MultiMapList<N, E>
) {

    companion object;
}

typealias Path<E> = List<E>

fun <N> Graph<N, *>.vertices(): Set<N> {
    return adjacencyList.keys
}

fun Graph<*, *>.numberOfEdges(): Int {
    return adjacencyList.values.sumOf { it.size }
}

fun interface EdgeVisitFilter<E : Edge<*>> {

    suspend fun shouldVisit(edge: E, pathPredecessor: E?): Boolean
}

/**
 * Finds all nodes reachable from [origin]
 *
 * Works for both directed and undirected graphs
 *
 * Complexity: O(V + E)
 */
suspend fun <N, E : Edge<N>> Graph<N, E>.findAllPossibleDestinations(
    origin: N,
    nodeVisitFilter: EdgeVisitFilter<E>? = null
): Set<N> {
    val actualNodeListFilter = nodeVisitFilter ?: EdgeVisitFilter { _, _ -> true }

    val reachableNodes = reachabilityDfs(origin, adjacencyList, actualNodeListFilter, predecessor = null)
    reachableNodes.removeAt(reachableNodes.indexOf(origin))

    return reachableNodes.toSet()
}

fun <N, E : Edge<N>> Graph<N, E>.hasOutcomingDirections(origin: N): Boolean {
    val vertices = adjacencyList[origin] ?: return false
    return vertices.isNotEmpty()
}

suspend fun <N, E : WeightedEdge<N>> Graph<N, E>.findDijkstraPathsBetween(
    from: N,
    to: N,
    limit: Int,
    nodeVisitFilter: EdgeVisitFilter<E>? = null
): List<Path<E>> {
    val actualNodeListFilter = nodeVisitFilter ?: EdgeVisitFilter { _, _ -> true }

    data class QueueElement(val currentPath: Path<E>, val score: Int) : Comparable<QueueElement> {

        override fun compareTo(other: QueueElement): Int {
            return score - other.score
        }

        fun lastNode(): N {
            return if (currentPath.isNotEmpty()) currentPath.last().to else from
        }

        operator fun contains(node: N): Boolean {
            return currentPath.any { it.from == node || it.to == node }
        }
    }

    val paths = mutableListOf<Path<E>>()

    val count = mutableMapOf<N, Int>()

    val heap = PriorityQueue<QueueElement>()
    heap.add(QueueElement(currentPath = emptyList(), score = 0))

    while (heap.isNotEmpty() && paths.size < limit) {
        val minimumQueueElement = heap.poll()!!
        val lastNode = minimumQueueElement.lastNode()

        val newCount = count.getOrElse(lastNode) { 0 } + 1
        count[lastNode] = newCount

        val predecessor = minimumQueueElement.currentPath.lastOrNull()

        if (lastNode == to) {
            paths.add(minimumQueueElement.currentPath)
            continue
        }

        if (newCount <= limit) {
            val edges = adjacencyList[lastNode].orEmpty()
            edges.forEach { edge ->
                if (edge.to in minimumQueueElement || !actualNodeListFilter.shouldVisit(edge, predecessor)) return@forEach

                val newElement = QueueElement(
                    currentPath = minimumQueueElement.currentPath + edge,
                    score = minimumQueueElement.score + edge.weightForAppendingTo(minimumQueueElement.currentPath)
                )

                heap.add(newElement)
            }
        }
    }

    return paths
}

private suspend fun <N, E : Edge<N>> reachabilityDfs(
    node: N,
    adjacencyList: Map<N, List<E>>,
    nodeVisitFilter: EdgeVisitFilter<E>,
    predecessor: E?,
    visited: MutableSet<N> = mutableSetOf(),
    connectedComponentState: MutableList<N> = mutableListOf()
): MutableList<N> {
    visited.add(node)
    connectedComponentState.add(node)

    val edges = adjacencyList[node].orEmpty()

    for (edge in edges) {
        if (edge.to !in visited && nodeVisitFilter.shouldVisit(edge, predecessor)) {
            reachabilityDfs(edge.to, adjacencyList, nodeVisitFilter, predecessor = edge, visited, connectedComponentState)
        }
    }

    return connectedComponentState
}
