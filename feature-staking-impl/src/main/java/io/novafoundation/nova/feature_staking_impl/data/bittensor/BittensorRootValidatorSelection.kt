package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BittensorRootValidatorSelection {
    private val selected = MutableStateFlow<String?>(null)

    val address = selected.asStateFlow()

    fun select(address: String) { selected.value = address }
}
