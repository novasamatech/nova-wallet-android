package io.novafoundation.nova.common.utils.graph

class CompoundEdgeVisitFilter<E : Edge<*>>(
    val filters: List<EdgeVisitFilter<E>>
) : EdgeVisitFilter<E> {

    constructor(vararg filters: EdgeVisitFilter<E>) : this(filters.toList())

    override suspend fun shouldVisit(edge: E, pathPredecessor: E?): Boolean {
        return filters.all { it.shouldVisit(edge, pathPredecessor) }
    }
}
