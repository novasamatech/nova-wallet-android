package io.novafoundation.nova.feature_swap_impl.presentation.common.route

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class SwapRouteModel(
    val chains: List<ChainUi>
)

typealias SwapRouteState = ExtendedLoadingState<SwapRouteModel?>
