package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.MultiMapList
import java.util.PriorityQueue

interface Edge<N> {

    val from: N

    val to: N
}

class Graph<N, E : Edge<N>>(
    val adjacencyList: Map<N, List<E>>
) {

    companion object;
}

fun <N, E : Edge<N>> Graph.Companion.create(vararg multiMaps: MultiMapList<N, E>): Graph<N, E> {
    return create(multiMaps.toList())
}

fun <N, E : Edge<N>> Graph.Companion.create(vararg adjacencyPairs: Pair<N, List<E>>): Graph<N, E> {
    return create(adjacencyPairs.toMap())
}

fun <N, E : Edge<N>> Graph.Companion.create(multiMaps: List<MultiMapList<N, E>>): Graph<N, E> {
    return GraphBuilder<N, E>().apply {
        multiMaps.forEach(::addEdges)
    }.build()
}

typealias ConnectedComponent<N> = List<N>
typealias Path<E> = List<E>

/**
 * Find all connected components of the graph.
 * Time Complexity is O(V+E)
 * Space Complexity is O(V)
 */
fun <N, E : Edge<N>> Graph<N, E>.findConnectedComponents(): List<ConnectedComponent<N>> {
    val visited = mutableSetOf<N>()
    val result = mutableListOf<ConnectedComponent<N>>()

    for (vertex in adjacencyList.keys) {
        if (vertex in visited) continue

        val nextConnectedComponent = connectedComponentsDfs(vertex, adjacencyList, visited)
        result.add(nextConnectedComponent)
    }

    return result
}

fun <N, E : Edge<N>> Graph<N, E>.findConnectedComponentFor(vertex: N): ConnectedComponent<N> {
    return connectedComponentsDfs(vertex, adjacencyList, visited = mutableSetOf())
}

fun <N, E : Edge<N>> Graph<N, E>.findAllPossibleDirections(): MultiMap<N, N> {
    val connectedComponents = findConnectedComponents()
    return connectedComponents.findAllPossibleDirections()
}

fun <N, E : Edge<N>> Graph<N, E>.findAllPossibleDirectionsToList(): MultiMapList<N, N> {
    val connectedComponents = findConnectedComponents()
    return connectedComponents.findAllPossibleDirectionsToList()
}

fun <N, E : Edge<N>> Graph<N, E>.findAllPossibleDirectionsFor(vertex: N): Set<N> {
    val connectedComponent = findConnectedComponentFor(vertex)
    return connectedComponent.findAllPossibleDirectionsFor(vertex)
}

fun <N> List<ConnectedComponent<N>>.findAllPossibleDirections(): MultiMap<N, N> {
    val result = mutableMapOf<N, Set<N>>()

    forEach { connectedComponent ->
        val asSet = connectedComponent.toSet()

        asSet.forEach { node ->
            // in the connected component every node is connected to every other except itself
            result[node] = asSet - node
        }
    }

    return result
}

fun <N> ConnectedComponent<N>.findAllPossibleDirectionsFor(node: N): Set<N> {
    val nodeSet = this.toSet()

    return if (nodeSet.contains(node)) {
        nodeSet - node
    } else {
        emptySet()
    }
}

fun <N> List<ConnectedComponent<N>>.findAllPossibleDirectionsToList(): MultiMapList<N, N> {
    val result = mutableMapOf<N, List<N>>()

    forEach { connectedComponent ->
        connectedComponent.forEach { node ->
            // in the connected component every node is connected to every other except itself
            result[node] = connectedComponent - node
        }
    }

    return result
}

fun <N, E : Edge<N>> Graph<N, E>.findDijkstraPathsBetween(from: N, to: N, limit: Int): List<Path<E>> {
    data class QueueElement(val currentPath: Path<E>, val nodeList: List<N>, val score: Int) : Comparable<QueueElement> {
        override fun compareTo(other: QueueElement): Int {
            return score - other.score
        }
    }

    val paths = mutableListOf<Path<E>>()

    val count = mutableMapOf<N, Int>()
    adjacencyList.keys.forEach { count[it] = 0 }

    val heap = PriorityQueue<QueueElement>()
    heap.add(QueueElement(currentPath = emptyList(), nodeList = listOf(from), score = 0))

    while (heap.isNotEmpty() && paths.size < limit) {
        val minimumQueueElement = heap.poll()!!
        val lastNode = minimumQueueElement.nodeList.last()

        val newCount = count.getValue(lastNode) + 1
        count[lastNode] = newCount

        if (lastNode == to) {
            paths.add(minimumQueueElement.currentPath)
            continue
        }

        if (newCount <= limit) {
            adjacencyList.getValue(lastNode).forEach { edge ->
                if (edge.to in minimumQueueElement.nodeList) return@forEach

                val newElement = QueueElement(
                    currentPath = minimumQueueElement.currentPath + edge,
                    nodeList = minimumQueueElement.nodeList + edge.to,
                    score = minimumQueueElement.score + 1
                )

                heap.add(newElement)
            }
        }
    }

    return paths
}

private fun <N, E : Edge<N>> connectedComponentsDfs(
    node: N,
    adjacencyList: Map<N, List<E>>,
    visited: MutableSet<N>,
    connectedComponentState: MutableList<N> = mutableListOf()
): ConnectedComponent<N> {
    visited.add(node)
    connectedComponentState.add(node)

    for (edge in adjacencyList.getValue(node)) {
        if (edge.to !in visited) {
            connectedComponentsDfs(edge.to, adjacencyList, visited, connectedComponentState)
        }
    }

    return connectedComponentState
}
