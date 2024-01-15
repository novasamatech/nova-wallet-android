package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed interface AddStakingProxyValidationFailure {

    class NotEnoughToPayFee(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : AddStakingProxyValidationFailure, NotEnoughToPayFeesError

    class NotEnoughToStayAboveED(override val asset: Chain.Asset) : AddStakingProxyValidationFailure, InsufficientBalanceToStayAboveEDError

    class NotEnoughBalanceToReserveDeposit(
        val chainAsset: Chain.Asset,
        val availableBalance: Balance,
        val deposit: Balance
    ) : AddStakingProxyValidationFailure

    class InvalidAddress(val chain: Chain) : AddStakingProxyValidationFailure

    class MaximumProxiesReached(val chain: Chain, val max: Int) : AddStakingProxyValidationFailure

    object AlreadyDelegated : AddStakingProxyValidationFailure
}
