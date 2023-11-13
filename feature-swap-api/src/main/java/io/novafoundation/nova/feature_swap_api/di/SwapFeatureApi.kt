package io.novafoundation.nova.feature_swap_api.di

import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider

interface SwapFeatureApi {

    val swapService: SwapService

    val swapSettingsStateProvider: SwapSettingsStateProvider

    val swapAvailabilityInteractor: SwapAvailabilityInteractor

    val swapRateFormatter: SwapRateFormatter
}
