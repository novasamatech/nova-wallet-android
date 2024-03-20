package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isFull
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isRewardedListFull
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.TooLowDelegation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.TooLowTotalStake
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake.WontReceiveRewards
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novasama.substrate_sdk_android.extensions.fromHex

class MinimumDelegationValidationFactory(
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) {

    fun ValidationSystemBuilder<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>.minimumDelegation() {
        validate(MinimumDelegationValidation(stakingConstantsRepository, candidatesRepository, delegatorStateUseCase))
    }
}

class MinimumDelegationValidation(
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
    private val delegatorStateUseCase: DelegatorStateUseCase,
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

        val candidateMetadata = value.collator.candidateMetadata
        val lowestBottomDelegationAmount = token.amountFromPlanks(candidateMetadata.lowestBottomDelegationAmount)

        val delegatorState = delegatorStateUseCase.currentDelegatorState()
        val asDelegator = delegatorState.castOrNull<DelegatorState.Delegator>()

        val totalDelegatedPlanks = asDelegator?.total.orZero()
        val totalDelegated = token.amountFromPlanks(totalDelegatedPlanks)

        val stakedInSelectedCollatorPlanks = asDelegator?.delegationAmountTo(collatorId).orZero()
        val stakedInSelectedCollator = token.amountFromPlanks(stakedInSelectedCollatorPlanks)

        return when {
            // amount is lower than minimum required delegation
            stakedInSelectedCollator + value.amount < minimumDelegationAmount -> {
                val needToStake = minimumDelegationAmount - stakedInSelectedCollator
                validationError(TooLowDelegation(needToStake, asset, strictGreaterThan = false))
            }

            // amount is lower than minimum total stake
            totalDelegated + value.amount < minimumTotalStakeAmount -> {
                val needToStake = minimumTotalStakeAmount - totalDelegated
                validationError(TooLowTotalStake(needToStake, asset))
            }

            // collator is full so we need strictly greater amount then minimum stake
            candidateMetadata.isFull() && value.amount + stakedInSelectedCollator <= lowestBottomDelegationAmount -> {
                validationError(TooLowDelegation(lowestBottomDelegationAmount, asset, strictGreaterThan = true))
            }

            // collator's top is full but we can still join bottom delegations
            candidateMetadata.isRewardedListFull() && value.amount + stakedInSelectedCollator <= minStakeToGetRewards -> {
                validationWarning(WontReceiveRewards(minStakeToGetRewards, asset))
            }

            // otherwise we join collator's top
            else -> valid()
        }
    }
}
