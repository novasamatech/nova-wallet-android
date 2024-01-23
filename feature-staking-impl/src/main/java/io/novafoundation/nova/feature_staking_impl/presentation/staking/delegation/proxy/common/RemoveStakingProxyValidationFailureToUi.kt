package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.RemoveStakingProxyValidationFailure.NotEnoughToPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.RemoveStakingProxyValidationFailure.NotEnoughToStayAboveED
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.RemoveStakingProxyValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun mapRemoveStakingProxyValidationFailureToUi(
    resourceManager: ResourceManager,
    failure: RemoveStakingProxyValidationFailure,
): TitleAndMessage {
    return when (failure) {
        is NotEnoughToPayFee -> handleNotEnoughFeeError(failure, resourceManager)

        is NotEnoughToStayAboveED -> handleInsufficientBalanceCommission(failure, resourceManager)
    }
}
