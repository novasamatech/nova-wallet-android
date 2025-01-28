package io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias RedeemMythosValidationSystem = ValidationSystem<RedeemMythosStakingValidationPayload, RedeemMythosStakingValidationFailure>
typealias RedeemMythosValidationSystemBuilder = ValidationSystemBuilder<RedeemMythosStakingValidationPayload, RedeemMythosStakingValidationFailure>

fun ValidationSystem.Companion.mythosRedeem(): RedeemMythosValidationSystem = ValidationSystem {
    enoughToPayFees()
}
private fun RedeemMythosValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = {
            RedeemMythosStakingValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = it.payload.asset.token.configuration,
                maxUsable = it.maxUsable,
                fee = it.fee
            )
        }
    )
}
