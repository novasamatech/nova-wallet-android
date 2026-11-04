package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.matchesQuote
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.ext.fullId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val BACKGROUND_QUOTE_REFRESH_INTERVAL = 10.seconds

private data class SwapFeeRequest(val quoteState: QuotingState, val settings: SwapSettings)

internal fun QuotingState.Loaded.matchesSettings(settings: SwapSettings): Boolean {
    return quoteArgs.tokenIn.configuration.fullId == settings.assetIn?.fullId &&
        quoteArgs.tokenOut.configuration.fullId == settings.assetOut?.fullId &&
        quoteArgs.amount == settings.amount && quoteArgs.swapDirection == settings.swapDirection
}

internal fun Flow<ReQuoteTrigger>.backgroundQuoteRefreshes(
    settings: Flow<SwapSettings>,
    quotingState: StateFlow<QuotingState>,
    fee: StateFlow<FeeStatus<SwapFee, FeeDisplay>>,
    isQuoteActive: () -> Boolean,
): Flow<SwapSettings> {
    return throttleLast(BACKGROUND_QUOTE_REFRESH_INTERVAL)
        .mapNotNull {
            val currentSettings = settings.first()
            if (isQuoteActive()) return@mapNotNull null

            val canRefresh = when (val quoteState = quotingState.value) {
                is QuotingState.Loaded -> {
                    val feeStatus = fee.value
                    val hasMatchingFee = feeStatus.loadedFeeOrNull()?.matchesQuote(quoteState.quote, currentSettings.slippage) == true
                    // Also closes the gap between publishing a quote and FeeLoader observing it.
                    quoteState.matchesSettings(currentSettings) && (feeStatus is FeeStatus.Error || hasMatchingFee)
                }

                // An RPC may cancel its request without closing the screen. The active-job guard
                // above protects a running quote; a finished job must not strand the UI in Loading.
                QuotingState.Loading -> true
                // These states own no loaded quote/fee pair. Allow subscription-driven recovery.
                QuotingState.Default, is QuotingState.Error -> true
            }

            currentSettings.takeIf { canRefresh }
        }
}

internal suspend fun collectSwapFees(
    quotingState: Flow<QuotingState>,
    settings: Flow<SwapSettings>,
    feeLoader: FeeLoaderMixinV2.Presentation<SwapFee, FeeDisplay>,
    estimateFee: suspend (SwapFeeArgs) -> SwapFee,
) {
    combine(quotingState, settings, ::SwapFeeRequest)
        .distinctUntilChanged()
        .collectLatest { (quoteState, currentSettings) ->
            val currentFee = quoteState is QuotingState.Loaded && quoteState.matchesSettings(currentSettings) &&
                feeLoader.fee.value.loadedFeeOrNull()?.matchesQuote(quoteState.quote, currentSettings.slippage) == true
            if (currentFee) return@collectLatest

            feeLoader.setFeeLoading()
            if (quoteState !is QuotingState.Loaded || !quoteState.matchesSettings(currentSettings)) {
                if (quoteState !is QuotingState.Loading) feeLoader.setFeeStatus(FeeStatus.NoFee)
                return@collectLatest
            }

            delay(300.milliseconds)
            feeLoader.loadFee { feePaymentCurrency ->
                estimateFee(quoteState.quote.toExecuteArgs(currentSettings.slippage, feePaymentCurrency))
            }
        }
}

internal fun DescriptiveButtonState.withSwapFeeReadiness(
    hasCurrentFee: Boolean,
    feeStatus: FeeStatus<*, *>,
): DescriptiveButtonState {
    if (this !is DescriptiveButtonState.Enabled) return this
    if (hasCurrentFee && feeStatus is FeeStatus.Loaded) return this

    return when (feeStatus) {
        FeeStatus.Error, FeeStatus.NoFee -> DescriptiveButtonState.Disabled(action)
        else -> DescriptiveButtonState.Loading
    }
}
