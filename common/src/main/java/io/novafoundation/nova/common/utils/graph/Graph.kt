package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.MultiMapList
import java.util.PriorityQueue

interface Edge<N> {

    val from: N

    val to: N
}

interface WeightedEdge<N> : Edge<N> {

    // Smaller the better
    val weight: Int
}

class Graph<N, E : Edge<N>>(
    val adjacencyList: MultiMapList<N, E>
) {

    companion object;
}

typealias ConnectedComponent<N> = List<N>
typealias Path<E> = List<E>

fun <N> Graph<N, *>.vertices(): Set<N> {
    return adjacencyList.keys
}

fun Graph<*, *>.numberOfEdges(): Int {
    return adjacencyList.values.sumOf { it.size }
}

fun interface NodeVisitFilter<N> {

    suspend fun shouldVisit(node: N): Boolean
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
    nodeVisitFilter: NodeVisitFilter<N>? = null
): Set<N> {
    val actualNodeListFilter =  nodeVisitFilter ?: NodeVisitFilter { true }

    return reachabilityDfs(origin, adjacencyList, actualNodeListFilter).toSet()
}

fun <N, E : Edge<N>> Graph<N, E>.hasOutcomingDirections(origin: N): Boolean {
    val vertices = adjacencyList[origin] ?: return false
    return vertices.isNotEmpty()
}


suspend fun <N, E : WeightedEdge<N>> Graph<N, E>.findDijkstraPathsBetween(
    from: N,
    to: N,
    limit: Int,
    nodeVisitFilter: NodeVisitFilter<N>?
): List<Path<E>> {
    val actualNodeListFilter =  nodeVisitFilter ?: NodeVisitFilter { true }

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
    adjacencyList.keys.forEach { count[it] = 0 }

    val heap = PriorityQueue<QueueElement>()
    heap.add(QueueElement(currentPath = emptyList(), score = 0))

    while (heap.isNotEmpty() && paths.size < limit) {
        val minimumQueueElement = heap.poll()!!
        val lastNode = minimumQueueElement.lastNode()

        val newCount = count.getValue(lastNode) + 1
        count[lastNode] = newCount

        if (lastNode == to) {
            paths.add(minimumQueueElement.currentPath)
            continue
        }

        if (newCount <= limit) {
            adjacencyList.getValue(lastNode).forEach { edge ->
                if (edge.to in minimumQueueElement || !actualNodeListFilter.shouldVisit(edge.to)) return@forEach

                val newElement = QueueElement(
                    currentPath = minimumQueueElement.currentPath + edge,
                    score = minimumQueueElement.score + edge.weight
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
    nodeVisitFilter: NodeVisitFilter<N>,
    visited: MutableSet<N> = mutableSetOf(),
    connectedComponentState: MutableList<N> = mutableListOf()
): List<N> {
    visited.add(node)
    connectedComponentState.add(node)

    for (edge in adjacencyList.getValue(node)) {
        if (edge.to !in visited && nodeVisitFilter.shouldVisit(node)) {
            reachabilityDfs(edge.to, adjacencyList, nodeVisitFilter, visited, connectedComponentState)
        }
    }

    return connectedComponentState
}


// TODO the commented code below doesn't work in a context of directed graphs !!!

///**
// * Find all connected components of the graph.
// * Time Complexity is O(V+E)
// * Space Complexity is O(V)
// */
//fun <N, E : Edge<N>> Graph<N, E>.findConnectedComponents(): List<ConnectedComponent<N>> {
//    val visited = mutableSetOf<N>()
//    val result = mutableListOf<ConnectedComponent<N>>()
//
//    for (vertex in adjacencyList.keys) {
//        if (vertex in visited) continue
//
//        val reachableNodes = reachabilityDfs(vertex, adjacencyList, visited)
//        result.add(reachableNodes)
//    }
//
//    return result
//}

//fun <N> List<ConnectedComponent<N>>.findAllPossibleDirectionsToList(): MultiMapList<N, N> {
//    val result = mutableMapOf<N, List<N>>()
//
//    forEach { connectedComponent ->
//        connectedComponent.forEach { node ->
//            // in the connected component every node is connected to every other except itself
//            result[node] = connectedComponent - node
//        }
//    }
//
//    return result
//}

//fun <N, E : Edge<N>> Graph<N, E>.findAllPossibleDirections(): MultiMap<N, N> {
//    val connectedComponents = findConnectedComponents()
//    return connectedComponents.findAllPossibleDirections()
//}

//fun <N, E : Edge<N>> Graph<N, E>.findAllPossibleDirectionsToList(): MultiMapList<N, N> {
//    val connectedComponents = findConnectedComponents()
//    return connectedComponents.findAllPossibleDirectionsToList()
//}

//fun <N> List<ConnectedComponent<N>>.findAllPossibleDirections(): MultiMap<N, N> {
//    val result = mutableMapOf<N, Set<N>>()
//
//    forEach { connectedComponent ->
//        val asSet = connectedComponent.toSet()
//
//        asSet.forEach { node ->
//            // in the connected component every node is connected to every other except itself
//            result[node] = asSet - node
//        }
//    }
//
//    return result
//}
