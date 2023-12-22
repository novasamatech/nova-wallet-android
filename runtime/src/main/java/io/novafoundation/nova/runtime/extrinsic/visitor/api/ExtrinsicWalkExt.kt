package io.novafoundation.nova.runtime.extrinsic.visitor.api

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents

suspend fun ExtrinsicWalk.walkToList(source: ExtrinsicWithEvents, chainId: ChainId): List<ExtrinsicVisit> {
    return buildList {
        walk(source, chainId) { visitedCall ->
            add(visitedCall)
        }
    }
}
