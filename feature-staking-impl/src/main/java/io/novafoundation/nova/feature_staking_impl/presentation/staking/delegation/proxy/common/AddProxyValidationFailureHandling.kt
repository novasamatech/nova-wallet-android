package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationFailure.NotEnoughBalanceToReserveDeposit
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationFailure.InvalidAddress
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationFailure.MaximumProxiesReached
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationFailure.NotEnoughToPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationFailure.NotEnoughToStayAboveED
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun mapAddStakingProxyValidationFailureToUi(
    resourceManager: ResourceManager,
    failure: AddStakingProxyValidationFailure,
): TitleAndMessage {
    return when (failure) {
        is NotEnoughBalanceToReserveDeposit -> resourceManager.getString(R.string.common_error_not_enough_tokens) to
            resourceManager.getString(
                R.string.staking_not_enough_balance_to_pay_proxy_deposit_message,
                failure.deposit.formatPlanks(failure.chainAsset),
                failure.availableBalance.formatPlanks(failure.chainAsset)
            )

        is InvalidAddress -> resourceManager.getString(R.string.invalid_proxy_address_title) to
            resourceManager.getString(R.string.invalid_proxy_address_message, failure.chain.name)

        is MaximumProxiesReached -> resourceManager.getString(R.string.add_proxy_maximum_reached_error_title) to
            resourceManager.getString(R.string.add_proxy_maximum_reached_error_message, failure.max, failure.chain.name)

        is NotEnoughToPayFee -> handleNotEnoughFeeError(failure, resourceManager)

        is NotEnoughToStayAboveED -> handleInsufficientBalanceCommission(failure, resourceManager)

        is AddStakingProxyValidationFailure.AlreadyDelegated -> resourceManager.getString(R.string.duplicate_proxy_type_title) to
            resourceManager.getString(R.string.duplicate_proxy_type_message, failure.address)
    }
}
