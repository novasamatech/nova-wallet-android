package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory
import okhttp3.HttpUrl

data class BittensorHistoryRequest internal constructor(
    val address: String,
    val baseUrl: HttpUrl,
    internal val generation: Long,
    val metaAccountId: Long
)

data class BittensorHistoryState(
    val result: Result<BittensorOperationHistory>?,
    val isSettling: Boolean
)
