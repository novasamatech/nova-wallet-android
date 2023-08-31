package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias SetupStakingValidationSystem = ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>
typealias SetupStakingValidationSystemBuilder = ValidationSystemBuilder<SetupStakingPayload, SetupStakingValidationFailure>

fun ValidationSystem.Companion.changeValidators(
    stakingRepository: StakingRepository,
    stakingSharedComputation: StakingSharedComputation,
): SetupStakingValidationSystem = ValidationSystem {
    enoughToPayFee()

    minimumBondValidation(
        stakingRepository = stakingRepository,
        stakingSharedComputation = stakingSharedComputation,
        chainAsset = { it.stashAsset.token.configuration },
        balanceToCheckAgainstRequired = { it.stashAsset.bondedInPlanks },
        balanceToCheckAgainstRecommended = { null }, // while changing validators we don't check against recommended minimum
        error = SetupStakingValidationFailure::AmountLessThanMinimum
    )
}

private fun SetupStakingValidationSystemBuilder.enoughToPayFee() {
    sufficientBalance(
        fee = { it.maxFee },
        available = { it.controllerAsset.transferable },
        error = { _, _ -> SetupStakingValidationFailure.CannotPayFee }
    )
}
