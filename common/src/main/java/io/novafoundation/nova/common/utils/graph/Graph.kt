package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.MultiMapList
import java.util.ArrayDeque
import java.util.Queue

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
fun <N, E: Edge<N>> Graph<N, E>.findConnectedComponents(): List<ConnectedComponent<N>> {
    val visited = mutableSetOf<N>()
    val result = mutableListOf<ConnectedComponent<N>>()

    for (vertex in adjacencyList.keys) {
        if (vertex in visited) continue

        val nextConnectedComponent = connectedComponentsDfs(vertex, adjacencyList, visited)
        result.add(nextConnectedComponent)
    }

    return result
}

fun  <N, E: Edge<N>> Graph<N, E>.findAllPossibleDirections(): MultiMap<N, N> {
    val connectedComponents = findConnectedComponents()
    val result = mutableMapOf<N, Set<N>>()

    connectedComponents.forEach { connectedComponent ->
        val asSet = connectedComponent.toSet()

        asSet.forEach { node ->
            // in the connected component every node is connected to every other except itself
            result[node] = asSet - node
        }
    }

    return result
}

// TODO this is not as memory efficient as using DFS since we maintain a copy of currentPath and visited for each pending queue element
// Not sure if it is possible to improve memory consumption herу
fun <N, E: Edge<N>> Graph<N, E>.findBfsPathsBetween(from: N, to: N, limit: Int): List<Path<E>> {
    data class QueueElement(val node: N, val currentPath: Path<E>, val visited: Set<N>)

    val paths = mutableListOf<Path<E>>()

    val queue: Queue<QueueElement> = ArrayDeque()

    for (edge in adjacencyList.getValue(from)) {
        queue.offer(QueueElement(edge.to, listOf(edge), setOf(from)))
    }

    while (paths.size < limit && queue.isNotEmpty()) {
        val (currentNode, currentPath, visited) = queue.poll()!!

        if (currentNode == to) {
            paths.add(currentPath)
            continue
        }

        for (edge in adjacencyList.getValue(currentNode)) {
            if (edge.to in visited) continue

            queue.offer(QueueElement(edge.to, currentPath + edge, visited + currentNode))
        }
    }

    return paths
}


private fun <N, E: Edge<N>> connectedComponentsDfs(
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
