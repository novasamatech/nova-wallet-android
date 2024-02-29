package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed interface RemoveStakingProxyValidationFailure {

    class NotEnoughToPayFee(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : RemoveStakingProxyValidationFailure, NotEnoughToPayFeesError

    class NotEnoughToStayAboveED(override val asset: Chain.Asset, override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel) :
        RemoveStakingProxyValidationFailure, InsufficientBalanceToStayAboveEDError
}
