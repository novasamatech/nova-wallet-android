package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.isRedeemableIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.calculateDurationTill
import io.novafoundation.nova.feature_staking_impl.domain.common.eraTimeCalculator
import io.novafoundation.nova.feature_staking_impl.domain.common.getActiveEra
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PoolMemberMaxUnlockingLimitReached
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PoolUnlockChunksLimitReached
import io.novafoundation.nova.runtime.state.selectedOption

class PoolUnlockChunksLimitValidation(
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val stakingSharedComputation: StakingSharedComputation,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val stakingSharedState: StakingSharedState,
) : NominationPoolsUnbondValidation {

    override suspend fun validate(value: NominationPoolsUnbondValidationPayload): ValidationStatus<NominationPoolsUnbondValidationFailure> {
        val stakingOption = stakingSharedState.selectedOption()
        val chainId = stakingOption.assetWithChain.chain.id
        val poolId = value.poolMember.poolId
        val sharedComputationScope = value.sharedComputationScope

        val activeEra = stakingSharedComputation.getActiveEra(chainId, sharedComputationScope)

        val maxUnlockingChunks = stakingConstantsRepository.maxUnlockingChunks(chainId).toInt()

        // poolMember cannot exist without a pool so it is safe to apply non-null assertion here
        val bondedPoolLedger = nominationPoolSharedComputation.participatingBondedPoolLedger(poolId, chainId, sharedComputationScope)!!
        val poolUnlockChunks = bondedPoolLedger.unlocking

        val unlockListHasFreePlaces = poolUnlockChunks.size < maxUnlockingChunks
        val canRedeem = poolUnlockChunks.any { it.isRedeemableIn(activeEra) }

        val canAddNewUnlockChunk = unlockListHasFreePlaces || canRedeem

        return canAddNewUnlockChunk isTrueOrError {
            val eraTimeCalculator = stakingSharedComputation.eraTimeCalculator(stakingOption, sharedComputationScope)

            val nearestUnlockingEra = poolUnlockChunks.minOf { it.redeemEra }
            val estimatedAllowanceTime = eraTimeCalculator.calculateDurationTill(nearestUnlockingEra)

            PoolUnlockChunksLimitReached(estimatedAllowanceTime)
        }
    }
}

class PoolMemberUnlockChunksLimitValidation(
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val stakingRepository: StakingRepository,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    private val stakingSharedState: StakingSharedState,
) : NominationPoolsUnbondValidation {

    override suspend fun validate(value: NominationPoolsUnbondValidationPayload): ValidationStatus<NominationPoolsUnbondValidationFailure> {
        val stakingOption = stakingSharedState.selectedOption()
        val chainId = stakingOption.assetWithChain.chain.id

        val bondingDuration = stakingConstantsRepository.lockupPeriodInEras(chainId)
        val currentEra = stakingRepository.getCurrentEraIndex(chainId)

        val unbondEra = currentEra + bondingDuration

        val maxUnlockingChunks = nominationPoolGlobalsRepository.maxUnlockChunks(chainId).toInt()

        val poolMemberUnlockChunks = value.poolMember.unbondingEras.keys

        val unlockListHasFreePlaces = poolMemberUnlockChunks.size < maxUnlockingChunks
        val targetUnbondEraPresentInUnlockingList = unbondEra in poolMemberUnlockChunks

        val canAddNewUnlockChunk = unlockListHasFreePlaces || targetUnbondEraPresentInUnlockingList

        return canAddNewUnlockChunk isTrueOrError {
            PoolMemberMaxUnlockingLimitReached(maxUnlockingChunks)
        }
    }
}
