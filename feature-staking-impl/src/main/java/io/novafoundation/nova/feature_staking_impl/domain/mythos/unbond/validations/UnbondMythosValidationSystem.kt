package io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias UnbondMythosValidationSystem = ValidationSystem<UnbondMythosStakingValidationPayload, UnbondMythosStakingValidationFailure>
typealias UnbondMythosValidationSystemBuilder = ValidationSystemBuilder<UnbondMythosStakingValidationPayload, UnbondMythosStakingValidationFailure>
typealias UnbondMythosValidation = Validation<UnbondMythosStakingValidationPayload, UnbondMythosStakingValidationFailure>

fun ValidationSystem.Companion.mythosUnbond(
    releaseRequestLimitNotReachedValidation: MythosReleaseRequestLimitNotReachedValidationFactory
): UnbondMythosValidationSystem = ValidationSystem {
    releaseRequestLimitNotReachedValidation.releaseRequestsLimitNotReached()

    enoughToPayFees()
}

private fun UnbondMythosValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = {
            UnbondMythosStakingValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = it.payload.asset.token.configuration,
                maxUsable = it.maxUsable,
                fee = it.fee
            )
        }
    )
}
