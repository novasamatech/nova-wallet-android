package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations

import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PartialUnbondLeavesLessThanMinBond
import io.novafoundation.nova.feature_wallet_api.domain.validation.CrossMinimumBalanceValidation

class NominationPoolsUnbondValidationFactory(
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val stakingRepository: StakingRepository,
    private val stakingSharedComputation: StakingSharedComputation,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val stakingSharedState: StakingSharedState,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
) {

    context(NominationPoolsUnbondValidationSystemBuilder)
    fun poolCanUnbond() {
        validate(
            PoolUnlockChunksLimitValidation(
                stakingConstantsRepository = stakingConstantsRepository,
                stakingSharedComputation = stakingSharedComputation,
                nominationPoolSharedComputation = nominationPoolSharedComputation,
                stakingSharedState = stakingSharedState
            )
        )
    }

    context(NominationPoolsUnbondValidationSystemBuilder)
    fun poolMemberCanUnbond() {
        validate(
            PoolMemberUnlockChunksLimitValidation(
                stakingConstantsRepository = stakingConstantsRepository,
                stakingRepository = stakingRepository,
                nominationPoolGlobalsRepository = nominationPoolGlobalsRepository,
                stakingSharedState = stakingSharedState
            )
        )
    }

    context(NominationPoolsUnbondValidationSystemBuilder)
    fun partialUnbondLeavesMinBond() {
        validate(
            CrossMinimumBalanceValidation(
                minimumBalance = { nominationPoolGlobalsRepository.minJoinBond(it.asset.token.configuration.chainId) },
                chainAsset = { it.asset.token.configuration },
                currentBalance = { it.stakedBalance },
                deductingAmount = { it.amount },
                error = ::PartialUnbondLeavesLessThanMinBond
            )
        )
    }
}
