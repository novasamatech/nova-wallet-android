package io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.common.validation.profitableAction
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias MythosClaimRewardsValidationSystem =
    ValidationSystem<MythosClaimRewardsValidationPayload, MythosClaimRewardsValidationFailure>

typealias MythosClaimRewardsValidationSystemBuilder =
    ValidationSystemBuilder<MythosClaimRewardsValidationPayload, MythosClaimRewardsValidationFailure>

fun ValidationSystem.Companion.mythosClaimRewards(): MythosClaimRewardsValidationSystem = ValidationSystem {
    enoughToPayFees()

    profitableClaim()
}

private fun MythosClaimRewardsValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { context ->
            MythosClaimRewardsValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.maxUsable,
                fee = context.fee
            )
        }
    )
}

private fun MythosClaimRewardsValidationSystemBuilder.profitableClaim() {
    profitableAction(
        amount = { pendingRewards },
        fee = { it.fee },
        error = { MythosClaimRewardsValidationFailure.NonProfitableClaim }
    )
}
