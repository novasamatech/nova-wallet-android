package io.novafoundation.nova.feature_swap_core.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class QuotePath(val segments: List<Segment>) {

    class Segment(val from: FullChainAssetId, val to: FullChainAssetId, val sourceId: String, val sourceParams: Map<String, String>)
}
