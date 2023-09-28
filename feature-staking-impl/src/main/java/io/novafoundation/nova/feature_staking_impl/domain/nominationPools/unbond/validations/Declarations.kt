package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

typealias NominationPoolsUnbondValidationSystem = ValidationSystem<NominationPoolsUnbondValidationPayload, NominationPoolsUnbondValidationFailure>
typealias NominationPoolsUnbondValidationSystemBuilder =
    ValidationSystemBuilder<NominationPoolsUnbondValidationPayload, NominationPoolsUnbondValidationFailure>

typealias NominationPoolsUnbondValidation = Validation<NominationPoolsUnbondValidationPayload, NominationPoolsUnbondValidationFailure>

fun ValidationSystem.Companion.nominationPoolsUnbond(
    unbondValidationFactory: NominationPoolsUnbondValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
): NominationPoolsUnbondValidationSystem = ValidationSystem {
    unbondValidationFactory.poolCanUnbond()

    unbondValidationFactory.poolMemberCanUnbond()

    enoughToUnbond()

    enoughToPayFees()

    sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

    positiveUnbond()

    unbondValidationFactory.partialUnbondLeavesMinBond()
}

private fun NominationPoolsUnbondValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            NominationPoolsUnbondValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = payload.asset.token.configuration,
                availableToPayFees = leftForFees,
                fee = payload.fee
            )
        }
    )
}

private fun NominationPoolsUnbondValidationSystemBuilder.enoughToUnbond() {
    sufficientBalance(
        available = { it.stakedBalance },
        amount = { it.amount },
        error = { _, _ -> NominationPoolsUnbondValidationFailure.NotEnoughToUnbond }
    )
}

private fun NominationPoolsUnbondValidationSystemBuilder.positiveUnbond() {
    positiveAmount(
        amount = { it.amount },
        error = { NominationPoolsUnbondValidationFailure.NotPositiveAmount }
    )
}

private fun NominationPoolsUnbondValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.fee },
        total = { it.asset.total },
        chainWithAsset = { ChainWithAsset(it.chain, it.chain.utilityAsset) },
        error = { NominationPoolsUnbondValidationFailure.ToStayAboveED(it.chain.utilityAsset) }
    )
}
