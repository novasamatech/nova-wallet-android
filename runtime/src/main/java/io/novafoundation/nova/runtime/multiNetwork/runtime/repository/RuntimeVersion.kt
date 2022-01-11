package io.novafoundation.nova.runtime.multiNetwork.runtime.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RuntimeVersion(
    val chainId: ChainId,
    val specVersion: Int
)
