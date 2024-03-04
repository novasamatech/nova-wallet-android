package io.novafoundation.nova.feature_wallet_api.presentation.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount

fun handleInsufficientBalanceCommission(failure: InsufficientBalanceToStayAboveEDError, resourceManager: ResourceManager): TitleAndMessage {
    return resourceManager.getString(R.string.common_too_small_balance_title) to
        resourceManager.getString(
            R.string.wallet_send_insufficient_balance_commission,
            failure.errorModel.minRequiredBalance.formatTokenAmount(failure.asset),
            failure.errorModel.availableBalance.formatTokenAmount(failure.asset),
            failure.errorModel.balanceToAdd.formatTokenAmount(failure.asset),
        )
}

fun handleNonPositiveAmount(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.common_error_general_title),
        resourceManager.getString(R.string.common_zero_amount_error)
    )
}
