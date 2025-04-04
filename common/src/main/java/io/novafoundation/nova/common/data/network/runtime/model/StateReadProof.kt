package io.novafoundation.nova.common.data.network.runtime.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash

class StateReadProof(
    val at: BlockHash,
    val proof: List<String>
)
