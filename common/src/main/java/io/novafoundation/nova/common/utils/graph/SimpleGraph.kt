package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.MultiMapList

data class SimpleEdge<N>(override val from: N, override val to: N) : WeightedEdge<N> {

    override fun weightForAppendingTo(path: Path<WeightedEdge<N>>): Int {
        return 1
    }
}

typealias SimpleGraph<N> = Graph<N, SimpleEdge<N>>

fun <N> Graph.Companion.createSimple(vararg multiMaps: MultiMapList<N, N>): SimpleGraph<N> {
    return createSimple(multiMaps.toList())
}

fun <N> Graph.Companion.createSimple(vararg adjacencyPairs: Pair<N, List<N>>): SimpleGraph<N> {
    return createSimple(adjacencyPairs.toMap())
}

fun <N> Graph.Companion.createSimple(multiMaps: List<MultiMapList<N, N>>): SimpleGraph<N> {
    return GraphBuilder<N, SimpleEdge<N>>().apply {
        multiMaps.forEach(::addSimpleEdges)
    }.build()
}

fun <N> GraphBuilder<N, SimpleEdge<N>>.addSimpleEdges(map: MultiMapList<N, N>) {
    map.forEach { (fromNode, toNodes) ->
        addSimpleEdges(fromNode, toNodes)
    }
}

fun <N> GraphBuilder<N, SimpleEdge<N>>.addSimpleEdges(from: N, to: List<N>) {
    addEdges(from, to.map { SimpleEdge(from, it) })
}
