package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class NominationPoolsBondMoreValidationFailure {

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val availableToPayFees: BigDecimal,
        override val fee: BigDecimal
    ) : NominationPoolsBondMoreValidationFailure(), NotEnoughToPayFeesError

    object NotEnoughToBond : NominationPoolsBondMoreValidationFailure()

    object NotPositiveAmount : NominationPoolsBondMoreValidationFailure()

    object PoolIsDestroying : NominationPoolsBondMoreValidationFailure()
}
