package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolAvailableBalanceValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolStateValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.validateNotDestroying
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount

typealias NominationPoolsBondMoreValidation = Validation<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>
typealias NominationPoolsBondMoreValidationSystem = ValidationSystem<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>
typealias NominationPoolsBondMoreValidationSystemBuilder =
    ValidationSystemBuilder<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>

fun ValidationSystem.Companion.nominationPoolsBondMore(
    poolStateValidationFactory: PoolStateValidationFactory,
    poolAvailableBalanceValidationFactory: PoolAvailableBalanceValidationFactory,
    stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory
): NominationPoolsBondMoreValidationSystem = ValidationSystem {
    noStakingTypesConflict(stakingTypesConflictValidationFactory)

    poolIsNotDestroying(poolStateValidationFactory)

    notUnstakingAll()

    enoughAvailableToStakeInPool(poolAvailableBalanceValidationFactory)

    positiveBond()
}

private fun NominationPoolsBondMoreValidationSystemBuilder.noStakingTypesConflict(factory: StakingTypesConflictValidationFactory) {
    factory.noStakingTypesConflict(
        accountId = { it.poolMember.accountId },
        chainId = { it.asset.token.configuration.chainId },
        error = { NominationPoolsBondMoreValidationFailure.StakingTypesConflict }
    )
}

private fun NominationPoolsBondMoreValidationSystemBuilder.poolIsNotDestroying(factory: PoolStateValidationFactory) {
    factory.validateNotDestroying(
        poolId = { it.poolMember.poolId },
        chainId = { it.asset.token.configuration.chainId },
        error = { NominationPoolsBondMoreValidationFailure.PoolIsDestroying }
    )
}

private fun NominationPoolsBondMoreValidationSystemBuilder.enoughAvailableToStakeInPool(factory: PoolAvailableBalanceValidationFactory) {
    factory.enoughAvailableBalanceToStake(
        asset = { it.asset },
        fee = { it.fee },
        amount = { it.amount },
        error = NominationPoolsBondMoreValidationFailure::NotEnoughToBond
    )
}

private fun NominationPoolsBondMoreValidationSystemBuilder.positiveBond() {
    positiveAmount(
        amount = { it.amount },
        error = { NominationPoolsBondMoreValidationFailure.NotPositiveAmount }
    )
}
