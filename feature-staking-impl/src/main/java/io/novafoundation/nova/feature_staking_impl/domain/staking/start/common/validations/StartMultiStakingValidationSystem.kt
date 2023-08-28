package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias StartMultiStakingValidation = Validation<StartMultiStakingValidationPayload, StartMultiStakingValidationFailure>
typealias StartMultiStakingValidationSystem = ValidationSystem<StartMultiStakingValidationPayload, StartMultiStakingValidationFailure>
typealias StartMultiStakingValidationSystemBuilder = ValidationSystemBuilder<StartMultiStakingValidationPayload, StartMultiStakingValidationFailure>

fun StartMultiStakingValidationSystemBuilder.enoughToPayFee() {
    sufficientBalance(
        fee = { it.fee.decimalAmount },
        available = { it.asset.transferable },
        error = { payload, availableToPayFees ->
            StartMultiStakingValidationFailure.NotEnoughToPayFees(
                chainAsset = payload.asset.token.configuration,
                availableToPayFees = availableToPayFees,
                fee = payload.fee.decimalAmount
            )
        }
    )
}

fun StartMultiStakingValidationSystemBuilder.positiveBond() {
    positiveAmount(
        amount = { it.amountOf(it.selection.stake) },
        error = { StartMultiStakingValidationFailure.NonPositiveAmount }
    )
}
