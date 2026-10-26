package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Confirmed own operations only; block-driven balance updates are not history invalidations. */
class BittensorPositionUpdates {

    data class Update(val metaId: Long, val chainId: String, val coldkey: AccountIdKey?)

    private val mutableUpdates = MutableSharedFlow<Update>(extraBufferCapacity = 64)

    val updates = mutableUpdates.asSharedFlow()

    fun confirmed(metaId: Long, chainId: String, coldkey: AccountIdKey?) {
        mutableUpdates.tryEmit(Update(metaId, chainId, coldkey))
    }
}
