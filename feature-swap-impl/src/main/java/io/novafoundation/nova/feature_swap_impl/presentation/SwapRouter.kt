package io.novafoundation.nova.feature_swap_impl.presentation

import kotlinx.coroutines.flow.Flow
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface SwapRouter : ReturnableRouter {

    fun openSwapConfirmation(payload: SwapOperationPayload)

    fun openSwapRoute(payload: SwapOperationPayload)

    fun openSwapFee(payload: SwapOperationPayload)

    fun openSwapExecution(payload: SwapOperationPayload)

    fun observeSwapRetries(): Flow<SwapSettingsPayload.RepeatOperation>

    fun openRetrySwap(payload: SwapSettingsPayload.RepeatOperation)

    fun openBalanceDetails(assetPayload: AssetPayload)

    fun openMain()
}
