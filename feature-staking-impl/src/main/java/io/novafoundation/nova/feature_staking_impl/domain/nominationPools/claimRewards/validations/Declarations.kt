package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.common.validation.profitableAction
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.domain.validation.enoughTotalToStayAboveED
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.runtime.ext.utilityAsset

typealias NominationPoolsClaimRewardsValidationSystem =
    ValidationSystem<NominationPoolsClaimRewardsValidationPayload, NominationPoolsClaimRewardsValidationFailure>

typealias NominationPoolsClaimRewardsValidationSystemBuilder =
    ValidationSystemBuilder<NominationPoolsClaimRewardsValidationPayload, NominationPoolsClaimRewardsValidationFailure>

fun ValidationSystem.Companion.nominationPoolsClaimRewards(
    assetSourceRegistry: AssetSourceRegistry
): NominationPoolsClaimRewardsValidationSystem = ValidationSystem {
    enoughToPayFees()

    sufficientCommissionBalanceToStayAboveED(assetSourceRegistry)

    profitableClaim()
}

private fun NominationPoolsClaimRewardsValidationSystemBuilder.sufficientCommissionBalanceToStayAboveED(
    assetSourceRegistry: AssetSourceRegistry
) {
    enoughTotalToStayAboveED(
        fee = { it.fee },
        total = { it.asset.total },
        existentialDeposit = { assetSourceRegistry.existentialDeposit(it.chain, it.chain.utilityAsset) },
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
