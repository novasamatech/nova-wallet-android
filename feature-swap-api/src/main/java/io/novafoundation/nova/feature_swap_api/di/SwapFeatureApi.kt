package io.novafoundation.nova.feature_swap_api.di

import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService

interface SwapFeatureApi {

    val swapService: SwapService
}
