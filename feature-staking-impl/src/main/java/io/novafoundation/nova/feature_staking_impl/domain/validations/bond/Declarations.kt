package io.novafoundation.nova.feature_staking_impl.domain.validations.bond

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.common.stakeable
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias BondMoreValidationSystem = ValidationSystem<BondMoreValidationPayload, BondMoreValidationFailure>
typealias BondMoreValidationSystemBuilder = ValidationSystemBuilder<BondMoreValidationPayload, BondMoreValidationFailure>

fun ValidationSystem.Companion.bondMore(): BondMoreValidationSystem = ValidationSystem {
    enoughToPayFees()

    enoughStakeable()

    positiveBond()
}

private fun BondMoreValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.stashAsset.transferable },
        error = { BondMoreValidationFailure.NOT_ENOUGH_TO_PAY_FEES }
    )
}

private fun BondMoreValidationSystemBuilder.enoughStakeable() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.stashAsset.stakeable },
        amount = { it.amount },
        error = { BondMoreValidationFailure.NOT_ENOUGH_STAKEABLE }
    )
}

private fun BondMoreValidationSystemBuilder.positiveBond() {
    positiveAmount(
        amount = { it.amount },
        error = { BondMoreValidationFailure.ZERO_BOND }
    )
}
