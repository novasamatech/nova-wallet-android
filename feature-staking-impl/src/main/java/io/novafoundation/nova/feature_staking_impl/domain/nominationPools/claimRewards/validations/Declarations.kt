package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.common.validation.profitableAction
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

typealias NominationPoolsClaimRewardsValidationSystem =
    ValidationSystem<NominationPoolsClaimRewardsValidationPayload, NominationPoolsClaimRewardsValidationFailure>

typealias NominationPoolsClaimRewardsValidationSystemBuilder =
    ValidationSystemBuilder<NominationPoolsClaimRewardsValidationPayload, NominationPoolsClaimRewardsValidationFailure>

fun ValidationSystem.Companion.nominationPoolsClaimRewards(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
): NominationPoolsClaimRewardsValidationSystem = ValidationSystem {
    enoughToPayFees()

    sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

    profitableClaim()
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.fee },
        total = { it.asset.total },
        chainWithAsset = { ChainWithAsset(it.chain, it.chain.utilityAsset) },
        error = { NominationPoolsClaimRewardsValidationFailure.ToStayAboveED(it.chain.utilityAsset) }
    )
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            NominationPoolsClaimRewardsValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = payload.asset.token.configuration,
                availableToPayFees = leftForFees,
                fee = payload.fee
            )
        }
    )
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.profitableClaim() {
    profitableAction(
        amount = { pendingRewards },
        fee = { fee },
        error = { NominationPoolsClaimRewardsValidationFailure.NonProfitableClaim }
    )
}
