package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isFull
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.TooLowDelegation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.TooLowTotalStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.WontReceiveRewards
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import jp.co.soramitsu.fearless_utils.extensions.fromHex

class MinimumDelegationValidationFactory(
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
) {

    fun ValidationSystemBuilder<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>.minimumDelegation() {
        validate(MinimumDelegationValidation(stakingConstantsRepository, candidatesRepository))
    }
}

class MinimumDelegationValidation(
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
) : StartParachainStakingValidation {

    override suspend fun validate(value: StartParachainStakingValidationPayload): ValidationStatus<StartParachainStakingValidationFailure> {
        val asset = value.asset
        val token = asset.token
        val chainId = token.configuration.chainId

        val collatorId = value.collator.accountIdHex.fromHex()

        val minimumDelegationInPlanks = stakingConstantsRepository.minimumDelegation(chainId)
        val minimumDelegationAmount = token.amountFromPlanks(minimumDelegationInPlanks)

        val minimumTotalStakeInPlanks = stakingConstantsRepository.minimumDelegatorStake(chainId)
        val minimumTotalStakeAmount = token.amountFromPlanks(minimumTotalStakeInPlanks)

        val minStakeToGetRewards = token.amountFromPlanks(value.collator.minimumStakeToGetRewards.orZero())
        val maxAllowedDelegators = stakingConstantsRepository.maxTotalDelegatorsPerCollator(chainId)

        val candidateMetadata = candidatesRepository.getCandidateMetadata(chainId, collatorId)
        val lowestBottomDelegationAmount = token.amountFromPlanks(candidateMetadata.lowestBottomDelegationAmount)

        return when {
            // amount is lower than minimum required delegation
            value.amount < minimumDelegationAmount -> validationError(TooLowDelegation(minimumDelegationAmount, asset, strictGreaterThan = false))

            // amount is lower than minimum total stake
            value.amount < minimumTotalStakeAmount -> validationError(TooLowTotalStake(minimumTotalStakeAmount, asset))

            // collator is full so we need strictly greater amount then minimum stake
            candidateMetadata.isFull(maxAllowedDelegators) && value.amount <= lowestBottomDelegationAmount -> {
                validationError(TooLowDelegation(lowestBottomDelegationAmount, asset, strictGreaterThan = true))
            }

            // otherwise we can join bottom delegations
            value.amount < minStakeToGetRewards -> validationWarning(WontReceiveRewards(minStakeToGetRewards, asset))

            else -> valid()
        }
    }
}
