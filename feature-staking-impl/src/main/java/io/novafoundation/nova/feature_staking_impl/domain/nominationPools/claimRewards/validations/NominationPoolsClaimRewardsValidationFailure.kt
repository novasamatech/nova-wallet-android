package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientTotalToStayAboveEDError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class NominationPoolsClaimRewardsValidationFailure {

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val availableToPayFees: BigDecimal,
        override val fee: BigDecimal
    ) : NominationPoolsClaimRewardsValidationFailure(), NotEnoughToPayFeesError

    object NonProfitableClaim : NominationPoolsClaimRewardsValidationFailure()

    class ToStayAboveED(override val asset: Chain.Asset) : NominationPoolsClaimRewardsValidationFailure(), InsufficientTotalToStayAboveEDError
}
