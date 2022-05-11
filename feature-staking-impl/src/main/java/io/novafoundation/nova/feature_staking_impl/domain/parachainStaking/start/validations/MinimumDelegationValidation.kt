package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.TooLowDelegation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.TooLowTotalStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.WontReceiveRewards
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks

class MinimumDelegationValidation(
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
) : StartParachainStakingValidation {

    override suspend fun validate(value: StartParachainStakingValidationPayload): ValidationStatus<StartParachainStakingValidationFailure> {
        val asset = value.asset
        val token = asset.token
        val chainId = token.configuration.chainId

        val minimumDelegationInPlanks = stakingConstantsRepository.minimumDelegation(chainId)
        val minimumDelegationAmount = token.amountFromPlanks(minimumDelegationInPlanks)

        val minimumTotalStakeInPlanks = stakingConstantsRepository.minimumDelegatorStake(chainId)
        val minimumTotalStakeAmount = token.amountFromPlanks(minimumTotalStakeInPlanks)

        val collatorMinStakeAmount = token.amountFromPlanks(value.collator.minimumStake)

        return when {
            value.amount < minimumDelegationAmount -> validationError(TooLowDelegation(minimumDelegationAmount, asset))
            value.amount < minimumTotalStakeAmount -> validationError(TooLowTotalStake(minimumTotalStakeAmount, asset))
            value.amount < collatorMinStakeAmount -> validationWarning(WontReceiveRewards(collatorMinStakeAmount, asset))
            else -> valid()
        }
    }
}

fun ValidationSystemBuilder<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>.minimumDelegation(
    stakingConstantsRepository: ParachainStakingConstantsRepository
) {
    validate(MinimumDelegationValidation(stakingConstantsRepository))
}
