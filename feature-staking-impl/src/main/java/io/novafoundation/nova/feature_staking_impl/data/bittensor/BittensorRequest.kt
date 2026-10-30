package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.io.IOException

const val BITTENSOR_REQUEST_TIMEOUT = 20_000L

/**
 * A Bittensor read ran out of its time budget - usually a slow chain node. Throw this instead of a bare `error(...)`
 * so screens can say the network is slow rather than showing a generic failure.
 */
class BittensorTimeoutException(operation: String, cause: Throwable? = null) : IOException("$operation timed out", cause)
