package io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class MythosClaimRewardsValidationFailure {

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : MythosClaimRewardsValidationFailure(), NotEnoughToPayFeesError

    object NonProfitableClaim : MythosClaimRewardsValidationFailure()
}
