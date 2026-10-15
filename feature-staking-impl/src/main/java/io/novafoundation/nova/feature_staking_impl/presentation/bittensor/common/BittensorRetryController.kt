package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.R
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Screen-local recovery for reads. Never retries a submitted transaction. */
class BittensorRetryController(private val resources: ResourceManager, private val logger: Logger) {

    private val errors = Channel<RetryPayload>(Channel.BUFFERED)

    val events = errors.receiveAsFlow()

    fun <T, R> observe(
        step: String,
        transform: (Flow<T>) -> Flow<R>,
        source: () -> Flow<T>
    ): Flow<R> {
        val reload = MutableStateFlow(0L)
        return reload.flatMapLatest {
            val attempt = flowOfAll { source() }
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    report(step, cause) { reload.update { it + 1 } }
                    throw cause
                }
            transform(attempt)
        }
    }

    private fun report(step: String, cause: Throwable, onRetry: () -> Unit) {
        logger.log("[Bittensor] Failed to load $step")
        logger.log(cause)
        errors.trySend(
            RetryPayload(
                title = resources.getString(R.string.common_error_general_title),
                message = resources.getString(R.string.common_retry_message),
                onRetry = onRetry
            )
        )
    }
}
