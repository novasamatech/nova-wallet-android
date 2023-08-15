package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolStateValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.validateNotDestroying
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias NominationPoolsBondMoreValidationSystem = ValidationSystem<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>
typealias NominationPoolsBondMoreValidationSystemBuilder =
    ValidationSystemBuilder<NominationPoolsBondMoreValidationPayload, NominationPoolsBondMoreValidationFailure>

fun ValidationSystem.Companion.nominationPoolsBondMore(
    poolStateValidationFactory: PoolStateValidationFactory,
): NominationPoolsBondMoreValidationSystem = ValidationSystem {
    enoughToBond()

    enoughToPayFees()

    positiveBond()

    poolIsNotDestroying(poolStateValidationFactory)
}

private fun NominationPoolsBondMoreValidationSystemBuilder.poolIsNotDestroying(factory: PoolStateValidationFactory) {
    factory.validateNotDestroying(
        poolId = { it.poolMember.poolId },
        chainId = { it.asset.token.configuration.chainId },
        error = { NominationPoolsBondMoreValidationFailure.PoolIsDestroying }
    )
}

private fun NominationPoolsBondMoreValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        amount = { it.amount },
        error = { payload, leftForFees ->
            NominationPoolsBondMoreValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = payload.asset.token.configuration,
                availableToPayFees = leftForFees,
                fee = payload.fee
            )
        }
    )
}

private fun NominationPoolsBondMoreValidationSystemBuilder.enoughToBond() {
    sufficientBalance(
        available = { it.asset.transferable },
        amount = { it.amount },
        error = { _, _ -> NominationPoolsBondMoreValidationFailure.NotEnoughToBond }
    )
}

private fun NominationPoolsBondMoreValidationSystemBuilder.positiveBond() {
    positiveAmount(
        amount = { it.amount },
        error = { NominationPoolsBondMoreValidationFailure.NotPositiveAmount }
    )
}
