package io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias ClaimContributionValidationSystem = ValidationSystem<ClaimContributionValidationPayload, ClaimContributionValidationFailure>
typealias ClaimContributionValidationSystemBuilder = ValidationSystemBuilder<ClaimContributionValidationPayload, ClaimContributionValidationFailure>

fun ValidationSystem.Companion.claimContribution(): ClaimContributionValidationSystem = ValidationSystem {
    enoughToPayFees()
}

private fun ClaimContributionValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = {
            ClaimContributionValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = it.payload.asset.token.configuration,
                maxUsable = it.maxUsable,
                fee = it.fee
            )
        }
    )
}
