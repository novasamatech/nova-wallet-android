package io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias StartMythosStakingValidationSystem = ValidationSystem<StartMythosStakingValidationPayload, StartMythosStakingValidationFailure>
typealias StartMythosStakingValidationSystemBuilder = ValidationSystemBuilder<StartMythosStakingValidationPayload, StartMythosStakingValidationFailure>
typealias StartMythosStakingValidation = Validation<StartMythosStakingValidationPayload, StartMythosStakingValidationFailure>

fun ValidationSystem.Companion.mythosStakingStart(
    minimumDelegationValidationFactory: MythosMinimumDelegationValidationFactory,
    hasPendingRewardsValidationFactory: MythosNoPendingRewardsValidationFactory
): StartMythosStakingValidationSystem = ValidationSystem {
    positiveAmount(
        amount = { it.amount },
        error = { StartMythosStakingValidationFailure.NotPositiveAmount }
    )

    minimumDelegationValidationFactory.minimumDelegation()

    hasPendingRewardsValidationFactory.noPendingRewards()

    enoughToPayFees()

    // We should have both this and enoughStakeableAfterFees since we want to show different error messages in those two different cases
    enoughStakeable()

    enoughStakeableAfterFees()
}

private fun StartMythosStakingValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = {
            StartMythosStakingValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = it.payload.asset.token.configuration,
                maxUsable = it.maxUsable,
                fee = it.fee
            )
        }
    )
}

private fun StartMythosStakingValidationSystemBuilder.enoughStakeableAfterFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.stakeableAmount() },
        amount = { it.amount },
        error = {
            StartMythosStakingValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = it.payload.asset.token.configuration,
                maxUsable = it.maxUsable,
                fee = it.fee
            )
        }
    )
}

private fun StartMythosStakingValidationSystemBuilder.enoughStakeable() {
    sufficientBalance(
        available = { it.stakeableAmount() },
        amount = { it.amount },
        error = { StartMythosStakingValidationFailure.NotEnoughStakeableBalance }
    )
}
