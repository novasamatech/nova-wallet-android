package io.novafoundation.nova.common.utils

import android.util.Log
import io.novasama.substrate_sdk_android.wsrpc.recovery.LinearReconnectStrategy
import io.novasama.substrate_sdk_android.wsrpc.recovery.ReconnectStrategy
import kotlinx.coroutines.delay

suspend inline fun <T> retryUntilDone(
    retryStrategy: ReconnectStrategy = LinearReconnectStrategy(step = 500L),
    block: () -> T,
): T {
    var attempt = 0

    while (true) {
        val blockResult = runCatching { block() }

        if (blockResult.isSuccess) {
            return blockResult.requireValue()
        } else {
            Log.e("RetryUntilDone", "Failed to execute retriable operation:", blockResult.requireException())

            attempt++

            delay(retryStrategy.getTimeForReconnect(attempt))
        }
    }
}
