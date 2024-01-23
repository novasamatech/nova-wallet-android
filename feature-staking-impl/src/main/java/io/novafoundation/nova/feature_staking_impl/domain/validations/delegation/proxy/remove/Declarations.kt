package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import java.math.BigDecimal

typealias RemoveStakingProxyValidationSystem = ValidationSystem<RemoveStakingProxyValidationPayload, RemoveStakingProxyValidationFailure>
typealias RemoveStakingProxyValidationSystemBuilder = ValidationSystemBuilder<RemoveStakingProxyValidationPayload, RemoveStakingProxyValidationFailure>

fun RemoveStakingProxyValidationSystemBuilder.sufficientBalanceToStayAboveEd(
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) = enoughTotalToStayAboveEDValidationFactory.validate(
    chainWithAsset = { ChainWithAsset(it.chain, it.asset.token.configuration) },
    balance = { it.asset.balanceCountedTowardsED() },
    fee = { it.fee },
    error = { payload, _ ->
        RemoveStakingProxyValidationFailure.NotEnoughToStayAboveED(
            asset = payload.asset.token.configuration
        )
    }
)

fun RemoveStakingProxyValidationSystemBuilder.sufficientBalanceToPayFee() {
    return sufficientBalance(
        available = { it.asset.free },
        amount = { BigDecimal.ZERO },
        fee = { it.fee },
        error = { context ->
            RemoveStakingProxyValidationFailure.NotEnoughToPayFee(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.maxUsable,
                fee = context.fee
            )
        }
    )
}
