package io.novafoundation.nova.feature_swap_impl.presentation.common.analytics

import io.novafoundation.nova.analytics.SwapSource
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapEntryPoint

/** The navigation payload carries where the user came from; analytics reports it verbatim. */
fun SwapEntryPoint.toAnalyticsSource(): SwapSource = when (this) {
    SwapEntryPoint.ASSET_DETAILS -> SwapSource.ASSET_DETAILS
    SwapEntryPoint.MAIN_SCREEN -> SwapSource.MAIN_SCREEN
    SwapEntryPoint.OPERATION_DETAILS -> SwapSource.OPERATION_DETAILS
    SwapEntryPoint.RETRY -> SwapSource.RETRY
}
