package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.enoughTotalToStayAboveED
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.runtime.ext.utilityAsset

typealias NominationPoolsRedeemValidationSystem = ValidationSystem<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>
typealias NominationPoolsRedeemValidationSystemBuilder = ValidationSystemBuilder<NominationPoolsRedeemValidationPayload, NominationPoolsRedeemValidationFailure>

fun ValidationSystem.Companion.nominationPoolsRedeem(assetSourceRegistry: AssetSourceRegistry): NominationPoolsRedeemValidationSystem = ValidationSystem {
    enoughToPayFees()
    sufficientCommissionBalanceToStayAboveED(assetSourceRegistry)
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

private fun NominationPoolsRedeemValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    assetSourceRegistry: AssetSourceRegistry
) {
    enoughTotalToStayAboveED(
        fee = { it.fee },
        total = { it.asset.total },
        existentialDeposit = { assetSourceRegistry.existentialDeposit(it.chain, it.chain.utilityAsset) },
        error = { NominationPoolsRedeemValidationFailure.ToStayAboveED(it.chain.utilityAsset) }
    )
}
