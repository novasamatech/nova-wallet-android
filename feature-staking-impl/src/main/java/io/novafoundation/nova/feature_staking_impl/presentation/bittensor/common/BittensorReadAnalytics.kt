package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import java.util.concurrent.atomic.AtomicBoolean
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorDataFailureReason
import io.novafoundation.nova.analytics.BittensorScreen
import java.io.IOException
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException

class BittensorReadAnalytics(
    private val analytics: AnalyticsService,
    private val endpoint: BittensorDataEndpoint,
    private val screen: BittensorScreen
) {
    private val failureReported = AtomicBoolean(false)

    fun failed(cause: Throwable) {
        val kind = cause.bittensorReadFailureKind()
        // As on iOS, a device the backend cannot serve is not a data failure.
        if (kind == BittensorReadFailureKind.DEVICE_UNSUPPORTED) return
        val reason = when {
            kind == BittensorReadFailureKind.REJECTED -> BittensorDataFailureReason.REJECTED
            // iOS reports any other error status, a rate limit included, as a server error.
            cause is HttpException -> BittensorDataFailureReason.SERVER_ERROR
            cause is IOException || cause is TimeoutCancellationException -> BittensorDataFailureReason.TRANSPORT
            cause is IllegalArgumentException -> BittensorDataFailureReason.CONTRACT_VIOLATION
            else -> BittensorDataFailureReason.SERVER_ERROR
        }
        if (failureReported.compareAndSet(false, true)) {
            analytics.track(BittensorAnalyticsEvent.DataFailed(endpoint, reason, screen, retryable = kind.retryable))
        }
    }

    fun retry() {
        failureReported.set(false)
        analytics.track(BittensorAnalyticsEvent.DataRetryTapped(endpoint, screen))
    }
}
