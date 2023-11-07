package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

typealias NominationPoolsRedeemValidationSystem = ValidationSystem<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>
typealias NominationPoolsRedeemValidationSystemBuilder = ValidationSystemBuilder<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>

fun ValidationSystem.Companion.nominationPoolsRedeem(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
): NominationPoolsRedeemValidationSystem = ValidationSystem {
    enoughToPayFees()
    sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)
}

private fun NominationPoolsRedeemValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            NominationPoolsRedeemValidationFailure.NotEnoughBalanceToPayFees(
                chainAsset = payload.asset.token.configuration,
                maxUsable = leftForFees,
                fee = payload.fee
            )
        }
    )
}

private fun NominationPoolsRedeemValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {
    enoughTotalToStayAboveEDValidationFactory.validate(
        fee = { it.fee },
        balance = { it.asset.total },
        chainWithAsset = { ChainWithAsset(it.chain, it.chain.utilityAsset) },
        error = { payload, _ -> NominationPoolsRedeemValidationFailure.ToStayAboveED(payload.chain.utilityAsset) }
    )
}
