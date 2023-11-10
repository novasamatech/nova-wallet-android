package io.novafoundation.nova.feature_wallet_api.presentation.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError

fun handleInsufficientBalanceCommission(failure: InsufficientBalanceToStayAboveEDError, resourceManager: ResourceManager): TitleAndMessage {
    return resourceManager.getString(R.string.common_not_enough_funds_title) to
        resourceManager.getString(R.string.wallet_send_insufficient_balance_commission, failure.asset.symbol)
}

fun handleNonPositiveAmount(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.common_error_general_title),
        resourceManager.getString(R.string.common_zero_amount_error)
    )
}
