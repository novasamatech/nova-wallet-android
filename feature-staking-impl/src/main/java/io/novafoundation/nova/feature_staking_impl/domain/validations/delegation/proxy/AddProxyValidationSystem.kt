package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class AddStakingProxyValidationPayload(
    val chain: Chain,
    val asset: Asset,
    val address: String,
    val fee: Fee,
    val deposit: Balance,
    val newProxyQuantity: Int,
)

sealed interface AddStakingProxyValidationFailure {

    class NotEnoughToPayFee(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : AddStakingProxyValidationFailure, NotEnoughToPayFeesError

    class NotEnoughBalanceToReserveDeposit(
        val chainAsset: Chain.Asset,
        val maxUsable: Balance,
        val deposit: Balance
    ) : AddStakingProxyValidationFailure

    class InvalidAddress(val chain: Chain) : AddStakingProxyValidationFailure

    class MaximumProxiesReached(val chain: Chain, val max: Int) : AddStakingProxyValidationFailure
}

typealias AddStakingProxyValidationSystem = ValidationSystem<AddStakingProxyValidationPayload, AddStakingProxyValidationFailure>
typealias AddStakingProxyValidationSystemBuilder = ValidationSystemBuilder<AddStakingProxyValidationPayload, AddStakingProxyValidationFailure>
