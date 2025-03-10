package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.common.validation.profitableAction
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
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
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory,
): NominationPoolsClaimRewardsValidationSystem = ValidationSystem {
    noStakingTypesConflict(stakingTypesConflictValidationFactory)

    enoughToPayFees()

    sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

    profitableClaim()
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.noStakingTypesConflict(factory: StakingTypesConflictValidationFactory) {
    factory.noStakingTypesConflict(
        accountId = { it.poolMember.accountId },
        chainId = { it.asset.token.configuration.chainId },
        error = { NominationPoolsClaimRewardsValidationFailure.StakingTypesConflict }
    )
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.fee },
        balance = { it.asset.balanceCountedTowardsED() },
        chainWithAsset = { ChainWithAsset(it.chain, it.chain.utilityAsset) },
        error = { payload, error -> NominationPoolsClaimRewardsValidationFailure.ToStayAboveED(payload.chain.utilityAsset, error) }
    )
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { context ->
            NominationPoolsClaimRewardsValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.maxUsable,
                fee = context.fee
            )
        }
    )
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.profitableClaim() {
    profitableAction(
        amount = { pendingRewards },
        fee = { it.fee },
        error = { NominationPoolsClaimRewardsValidationFailure.NonProfitableClaim }
    )
}
