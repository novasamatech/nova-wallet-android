package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationPrototype
import io.novafoundation.nova.feature_swap_api.domain.model.OnChainSwapEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge

/** Each index identifies the edge that produces the grouped operation's output. */
internal suspend fun Path<SwapGraphEdge>.operationPrototypes(): List<IndexedValue<AtomicSwapOperationPrototype>> = buildList {
    for ((index, edge) in this@operationPrototypes.withIndex()) {
        if (edge !is OnChainSwapEdge) continue

        val previous = lastOrNull()
        val appended = if (previous?.index == index - 1) edge.appendToOperationPrototype(previous.value) else null
        if (appended != null) {
            this[lastIndex] = IndexedValue(index, appended)
        } else {
            add(IndexedValue(index, edge.beginOperationPrototype()))
        }
    }
}

internal suspend fun Path<SwapGraphEdge>.outputCommission(): IndexedValue<AtomicSwapOperationPrototype>? =
    operationPrototypes().lastOrNull { it.value.chargesServiceFee }
