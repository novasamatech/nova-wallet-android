package io.novafoundation.nova.common.utils.graph

import io.novafoundation.nova.common.utils.MultiMapList

class GraphBuilder<N, E: Edge<N>> {

    private val adjacencyList: MutableMap<N, MutableList<E>>  = mutableMapOf()

    fun addEdge(from: N, to: E) {
        val fromEdges = adjacencyList.getOrPut(from) { mutableListOf() }
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

fun <N, E: Edge<N>> GraphBuilder<N, E>.addEdges(map: MultiMapList<N, E>) {
    map.forEach { (fromNode, toNodes) ->
        addEdges(fromNode, toNodes)
    }
}


