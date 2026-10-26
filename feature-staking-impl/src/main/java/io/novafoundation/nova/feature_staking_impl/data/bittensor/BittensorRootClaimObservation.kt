package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

data class BittensorRootClaimSnapshot(val claims: BittensorRootClaims?, val status: BittensorDataStatus)

internal fun observeBittensorRootClaims(
    blocks: Flow<*>,
    load: suspend () -> BittensorRootClaims,
    logger: Logger
): Flow<BittensorRootClaimSnapshot> = flow {
    var cached: BittensorRootClaims? = null
    emit(BittensorRootClaimSnapshot(null, BittensorDataStatus.Loading))
    blocks.conflate()
        .map {
            try {
                val claims = load()
                cached = claims
                BittensorRootClaimSnapshot(claims, BittensorDataStatus.Fresh)
            } catch (e: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                logger.log(e)
                BittensorRootClaimSnapshot(cached, BittensorDataStatus.Stale(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorClaims] Failed to refresh claim availability")
                logger.log(e)
                BittensorRootClaimSnapshot(cached, BittensorDataStatus.Stale(e))
            }
        }
        .retryWhen { cause, _ ->
            if (cause is CancellationException) throw cause
            logger.log(cause)
            emit(BittensorRootClaimSnapshot(cached, BittensorDataStatus.Stale(cause)))
            delay(SUBSCRIPTION_RETRY_MILLIS)
            true
        }
        .collect { emit(it) }
}

private const val SUBSCRIPTION_RETRY_MILLIS = 1_000L
