package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolDelegatedStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidation.ConflictingStakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

class StakingTypesConflictValidationFactory(
    private val stakingRepository: StakingRepository,
    private val delegatedStakeRepository: NominationPoolDelegatedStakeRepository,
    private val nominationPoolStakingRepository: NominationPoolMembersRepository
) {

    context(ValidationSystemBuilder<P, E>)
    fun <P, E> noStakingTypesConflict(
        accountId: suspend (P) -> AccountId,
        chainId: (P) -> ChainId,
        error: () -> E,
        checkStakingTypeNotPresent: ConflictingStakingType = ConflictingStakingType.DIRECT
    ) {
        validate(
            StakingTypesConflictValidation(
                accountId = accountId,
                chainId = chainId,
                error = error,
                stakingRepository = stakingRepository,
                delegatedStakeRepository = delegatedStakeRepository,
                nominationPoolStakingRepository = nominationPoolStakingRepository,
                checkStakingTypeNotPresent = checkStakingTypeNotPresent
            )
        )
    }
}

class StakingTypesConflictValidation<P, E>(
    private val accountId: suspend (P) -> AccountId,
    private val chainId: (P) -> ChainId,
    private val error: () -> E,
    private val stakingRepository: StakingRepository,
    private val nominationPoolStakingRepository: NominationPoolMembersRepository,
    private val delegatedStakeRepository: NominationPoolDelegatedStakeRepository,
    private val checkStakingTypeNotPresent: ConflictingStakingType
) : Validation<P, E> {

    enum class ConflictingStakingType {
        POOLS, DIRECT
    }

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chainId = chainId(value)
        val delegatedStakeSupported = delegatedStakeRepository.hasMigratedToDelegatedStake(chainId)
        if (!delegatedStakeSupported) return valid()

        val isConflictingTypePresent = checkStakingTypeNotPresent.checkPresent(chainId, accountId(value))

        return isConflictingTypePresent isFalseOrError error
    }

    private suspend fun ConflictingStakingType.checkPresent(chainId: ChainId, accountId: AccountId): Boolean {
        return when (this) {
            ConflictingStakingType.POOLS -> nominationPoolStakingRepository.getPoolMember(chainId, accountId) != null
            ConflictingStakingType.DIRECT -> stakingRepository.ledger(chainId, accountId) != null
        }
    }
}

fun handlePoolStakingTypesConflictValidationFailure(resourceManager: ResourceManager): TitleAndMessage {
    return resourceManager.getString(R.string.pool_staking_conflict_title) to
        resourceManager.getString(R.string.pool_staking_conflict_message)
}
