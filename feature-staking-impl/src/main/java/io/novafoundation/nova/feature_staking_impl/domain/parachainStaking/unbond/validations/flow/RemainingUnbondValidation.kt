package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow

import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.hasTheSaveValueAs
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.activeBonded
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isBottomDelegationsNotEmpty
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isRewardedListFull
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure.TooLowRemainingBond.WillBeAddedToUnbondings
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure.TooLowRemainingBond.WontReceiveRewards
import io.novasama.substrate_sdk_android.extensions.fromHex

class RemainingUnbondValidationFactory(
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) {

    fun ValidationSystemBuilder<ParachainStakingUnbondValidationPayload, ParachainStakingUnbondValidationFailure>.validRemainingUnbond() {
        validate(RemainingUnbondValidation(stakingConstantsRepository, candidatesRepository, delegatorStateUseCase))
    }
}

class RemainingUnbondValidation(
    private val stakingConstantsRepository: ParachainStakingConstantsRepository,
    private val candidatesRepository: CandidatesRepository,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) : ParachainStakingUnbondValidation {

    override suspend fun validate(
        value: ParachainStakingUnbondValidationPayload
    ): ValidationStatus<ParachainStakingUnbondValidationFailure> = with(value.asset.token) {
        val asset = value.asset
        val chainId = configuration.chainId

        val collatorId = value.collator.accountIdHex.fromHex()

        val minimumDelegationAmount = stakingConstantsRepository.minimumDelegation(chainId).toAmount()
        val minimumTotalStakeAmount = stakingConstantsRepository.minimumDelegatorStake(chainId).toAmount()
        val minStakeToGetRewards = value.collator.minimumStakeToGetRewards.orZero().toAmount()

        val candidateMetadata = candidatesRepository.getCandidateMetadata(chainId, collatorId)
        val highestBottomDelegationAmount = candidateMetadata.highestBottomDelegationAmount.toAmount()

        val delegatorState = delegatorStateUseCase.currentDelegatorState()
        val asDelegator = delegatorState.castOrNull<DelegatorState.Delegator>()

        val activeBondedAmount = asDelegator?.activeBonded.orZero().toAmount()
        val stakedInSelectedCollator = asDelegator?.delegationAmountTo(collatorId).orZero().toAmount()

        val isUnbondingAll = stakedInSelectedCollator hasTheSaveValueAs value.amount

        return when {
            // enough bonded balance to unbond specified amount
            value.amount > stakedInSelectedCollator -> {
                validationError(ParachainStakingUnbondValidationFailure.NotEnoughBondedToUnbond)
            }

            // remaining bond in selected collator will be less then minimum
            stakedInSelectedCollator - value.amount < minimumDelegationAmount && !isUnbondingAll -> {
                validationWarning(WillBeAddedToUnbondings(newAmount = stakedInSelectedCollator, minimumStake = minimumDelegationAmount, asset = asset))
            }

            // remaining total bond will be less then minimum
            activeBondedAmount - value.amount < minimumTotalStakeAmount && !isUnbondingAll -> {
                validationWarning(WillBeAddedToUnbondings(newAmount = stakedInSelectedCollator, minimumStake = minimumTotalStakeAmount, asset = asset))
            }

            // warn users if they will loose rewards by doing this unbond
            candidateMetadata.isRewardedListFull() && // only relevant if rewarded list is full
                candidateMetadata.isBottomDelegationsNotEmpty() && // there are delegators who can potentially kick user out from rewarded set
                stakedInSelectedCollator >= minStakeToGetRewards && // if user currently receives rewards
                stakedInSelectedCollator - value.amount < highestBottomDelegationAmount && // but will stop doing so after unbond
                !isUnbondingAll // only in case user unbonds not the whole amount
            -> {
                validationWarning(WontReceiveRewards(minStakeToGetRewards, asset))
            }

            // otherwise no consequences for user
            else -> valid()
        }
    }
}
