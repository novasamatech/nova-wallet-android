package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.MultiMapList

class GraphBuilder<N, E : Edge<N>> {

    private val adjacencyList: MutableMap<N, MutableList<E>> = mutableMapOf()

    fun addEdge(to: E) {
        val fromEdges = adjacencyList.getOrPut(to.from) { mutableListOf() }
        fromEdges.add(to)
    }

    fun addEdges(from: N, to: List<E>) {
        val fromEdges = adjacencyList.getOrPut(from) { mutableListOf() }
        fromEdges.addAll(to)
    }

    fun build(): Graph<N, E> {
        return Graph(adjacencyList)
    }
}

fun <N, E : Edge<N>> GraphBuilder<N, E>.addEdges(map: MultiMapList<N, E>) {
    map.forEach { (fromNode, toNodes) ->
        addEdges(fromNode, toNodes)
    }
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

fun <N, E : Edge<N>> Graph.Companion.create(edges: List<E>): Graph<N, E> {
    return build {
        edges.forEach {
            addEdge(it)
        }
    }
}

inline fun <N, E: Edge<N>> Graph.Companion.build(building: GraphBuilder<N, E>.() -> Unit): Graph<N, E> {
    return GraphBuilder<N, E>().apply(building).build()
}

inline fun <N, E: Edge<N>> Graph.Companion.buildAdjacencyList(building: GraphBuilder<N, E>.() -> Unit): MultiMapList<N, E> {
    return build(building).adjacencyList
}
