package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.utils.graph.Edge
import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SwapSelectionDirectionsTest {
    private data class TestEdge(override val from: String, override val to: String) : Edge<String>
    private val allowAll = EdgeVisitFilter<TestEdge> { _, _ -> true }
    private val graph = Graph.create(listOf(TestEdge("A", "B"), TestEdge("B", "C"), TestEdge("D", "B")))

    @Test
    fun `pay and receive selection use opposite directions`() = runBlocking<Unit> {
        assertEquals(setOf("A", "D"), graph.assetsForSelection("B", true, allowAll))
        assertEquals(setOf("C"), graph.assetsForSelection("B", false, allowAll))
    }

    @Test
    fun `same asset and unreachable tokens are excluded`() = runBlocking<Unit> {
        assertEquals(emptySet<String>(), graph.assetsForSelection("A", true, allowAll))
        assertEquals(emptySet<String>(), graph.assetsForSelection("C", false, allowAll))
        assertEquals(emptySet<String>(), graph.assetsForSelection("unknown", true, allowAll))
    }

    @Test
    fun `pay selection preserves predecessor dependent restrictions`() = runBlocking<Unit> {
        val filter = EdgeVisitFilter<TestEdge> { edge, previous -> edge.to != "C" || previous == null }
        assertEquals(setOf("B"), graph.assetsForSelection("C", true, filter))
    }

    @Test(expected = CancellationException::class)
    fun `route cancellation is not incompatibility`() = runBlocking<Unit> {
        val cancelled = EdgeVisitFilter<TestEdge> { _, _ -> throw CancellationException() }
        graph.assetsForSelection("C", true, cancelled)
    }
}
