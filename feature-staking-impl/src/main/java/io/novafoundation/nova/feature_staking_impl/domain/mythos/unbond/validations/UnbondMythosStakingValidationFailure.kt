package io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class UnbondMythosStakingValidationFailure {

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : UnbondMythosStakingValidationFailure(), NotEnoughToPayFeesError

    class ReleaseRequestsLimitReached(val limit: Int) : UnbondMythosStakingValidationFailure()
}
