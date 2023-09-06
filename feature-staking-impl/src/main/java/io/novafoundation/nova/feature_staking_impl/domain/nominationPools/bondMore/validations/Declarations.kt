package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolAvailableBalanceValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolStateValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.validateNotDestroying
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount

typealias NominationPoolsBondMoreValidation = Validation<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>
typealias NominationPoolsBondMoreValidationSystem = ValidationSystem<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>
typealias NominationPoolsBondMoreValidationSystemBuilder =
    ValidationSystemBuilder<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>

fun ValidationSystem.Companion.nominationPoolsBondMore(
    poolStateValidationFactory: PoolStateValidationFactory,
    poolAvailableBalanceValidationFactory: PoolAvailableBalanceValidationFactory,
): NominationPoolsBondMoreValidationSystem = ValidationSystem {
    poolIsNotDestroying(poolStateValidationFactory)

    notUnstakingAll()

    enoughAvailableToStakeInPool(poolAvailableBalanceValidationFactory)

    positiveBond()
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
        fee = { it.fee.fee.amount },
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
