package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

typealias NominationPoolsRedeemValidationSystem = ValidationSystem<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>
typealias NominationPoolsRedeemValidationSystemBuilder = ValidationSystemBuilder<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>

fun ValidationSystem.Companion.nominationPoolsRedeem(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory
): NominationPoolsRedeemValidationSystem = ValidationSystem {
    noStakingTypesConflict(stakingTypesConflictValidationFactory)

    enoughToPayFees()

    sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)
}

private fun NominationPoolsRedeemValidationSystemBuilder.noStakingTypesConflict(factory: StakingTypesConflictValidationFactory) {
    factory.noStakingTypesConflict(
        accountId = { it.poolMember.accountId },
        chainId = { it.asset.token.configuration.chainId },
        error = { NominationPoolsRedeemValidationFailure.StakingTypesConflict }
    )
}

private fun NominationPoolsRedeemValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { context ->
            NominationPoolsRedeemValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.maxUsable,
                fee = context.fee
            )
        }
    )
}

private fun NominationPoolsRedeemValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.fee },
        balance = { it.asset.balanceCountedTowardsED() },
        chainWithAsset = { ChainWithAsset(it.chain, it.chain.utilityAsset) },
        error = { payload, error -> NominationPoolsRedeemValidationFailure.ToStayAboveED(payload.chain.utilityAsset, error) }
    )
}
