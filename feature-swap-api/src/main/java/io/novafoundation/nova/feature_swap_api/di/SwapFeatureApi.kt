package io.novafoundation.nova.feature_swap_api.di

import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory

interface SwapFeatureApi {

    val swapService: SwapService

    val swapSettingsStateProvider: SwapSettingsStateProvider
}
