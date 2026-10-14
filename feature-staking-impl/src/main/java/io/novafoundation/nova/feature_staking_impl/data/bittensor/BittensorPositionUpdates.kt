package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BittensorPositionUpdates {

    private val revision = MutableStateFlow(0L)

    val updates = revision.asStateFlow()

    fun refresh() {
        revision.update { it + 1 }
    }
}
