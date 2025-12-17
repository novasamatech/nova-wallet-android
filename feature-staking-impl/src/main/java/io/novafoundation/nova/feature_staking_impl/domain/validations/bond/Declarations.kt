package io.novafoundation.nova.feature_staking_impl.domain.validations.bond

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

typealias BondMoreValidationSystem = ValidationSystem<BondMoreValidationPayload, BondMoreValidationFailure>
typealias BondMoreValidationSystemBuilder = ValidationSystemBuilder<BondMoreValidationPayload, BondMoreValidationFailure>

fun ValidationSystem.Companion.bondMore(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
): BondMoreValidationSystem = ValidationSystem {
    enoughToPayFees()

    enoughStakeable()

    sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

    positiveBond()
}

private fun BondMoreValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.stashAsset.transferable },
        error = { BondMoreValidationFailure.NotEnoughToPayFees }
    )
}

private fun BondMoreValidationSystemBuilder.enoughStakeable() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.stakeable },
        amount = { it.amount },
        error = { BondMoreValidationFailure.NotEnoughStakeable }
    )
}

private fun BondMoreValidationSystemBuilder.positiveBond() {
    positiveAmount(
        amount = { it.amount },
        error = { BondMoreValidationFailure.ZeroBond }
    )
}

fun BondMoreValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.fee },
        balance = { it.stashAsset.balanceCountedTowardsED() },
        chainWithAsset = { ChainWithAsset(it.chain, it.stashAsset.token.configuration) },
        error = { payload, error -> BondMoreValidationFailure.NotEnoughFundToStayAboveED(payload.stashAsset.token.configuration, error) }
    )
}
