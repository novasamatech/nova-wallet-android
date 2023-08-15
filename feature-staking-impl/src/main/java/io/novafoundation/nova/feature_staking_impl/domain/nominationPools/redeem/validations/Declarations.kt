package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias NominationPoolsRedeemValidationSystem = ValidationSystem<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>
typealias NominationPoolsRedeemValidationSystemBuilder = ValidationSystemBuilder<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>

fun ValidationSystem.Companion.nominationPoolsRedeem(): NominationPoolsRedeemValidationSystem = ValidationSystem {
    enoughToPayFees()
}

private fun NominationPoolsRedeemValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            NominationPoolsRedeemValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = payload.asset.token.configuration,
                availableToPayFees = leftForFees,
                fee = payload.fee
            )
        }
    )
}
