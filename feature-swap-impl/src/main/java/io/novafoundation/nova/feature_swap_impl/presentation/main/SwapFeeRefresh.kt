package io.novafoundation.nova.feature_swap_impl.presentation.main

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val BACKGROUND_FEE_REFRESH_INTERVAL = 10.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<QuotingState>.throttleFeeRefreshes(timeSource: TimeSource = TimeSource.Monotonic): Flow<QuotingState.Loaded> = flow {
    var lastFeeLoadStartedAt = timeSource.markNow() - BACKGROUND_FEE_REFRESH_INTERVAL
    var lastFeeQuote: QuotingState.Loaded? = null

    this@throttleFeeRefreshes
        .transformLatest { state ->
            if (state !is QuotingState.Loaded) return@transformLatest

            delay(300.milliseconds)
            val sameInputs = lastFeeQuote?.settings == state.settings && lastFeeQuote?.quoteArgs?.metaAccountId == state.quoteArgs.metaAccountId
            // A market update may replace a user quote during debounce; changed inputs must still bypass the interval.
            if (state.origin == QuoteRefreshOrigin.SUBSCRIPTION && sameInputs) {
                val remainingDelay = BACKGROUND_FEE_REFRESH_INTERVAL - lastFeeLoadStartedAt.elapsedNow()
                if (remainingDelay.isPositive()) delay(remainingDelay)
            }

            lastFeeLoadStartedAt = timeSource.markNow()
            lastFeeQuote = state
            emit(state)
        }
        .collect { emit(it) }
}
