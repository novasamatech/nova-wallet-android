package io.novafoundation.nova.runtime.multiNetwork.connection

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class NodeWithSaturatedUrl(
    val node: Chain.Node,
    val saturatedUrl: String
)
