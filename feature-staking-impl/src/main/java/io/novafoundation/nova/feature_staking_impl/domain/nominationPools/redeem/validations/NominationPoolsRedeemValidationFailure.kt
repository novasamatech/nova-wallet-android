package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientTotalToStayAboveEDError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class NominationPoolsRedeemValidationFailure {

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val availableToPayFees: BigDecimal,
        override val fee: BigDecimal
    ) : NominationPoolsRedeemValidationFailure(), NotEnoughToPayFeesError

    class ToStayAboveED(override val asset: Chain.Asset) : NominationPoolsRedeemValidationFailure(), InsufficientTotalToStayAboveEDError
}
