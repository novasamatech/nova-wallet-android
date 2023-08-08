package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.CrossMinimumBalanceValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.CrossMinimumBalanceValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlin.time.Duration

sealed class NominationPoolsUnbondValidationFailure {

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val availableToPayFees: BigDecimal,
        override val fee: BigDecimal
    ) : NominationPoolsUnbondValidationFailure(), NotEnoughToPayFeesError

    object NotEnoughToUnbond : NominationPoolsUnbondValidationFailure()

    object NotPositiveAmount : NominationPoolsUnbondValidationFailure()

    class PartialUnbondLeavesLessThanMinBond(override val errorContext: CrossMinimumBalanceValidation.ErrorContext) :
        NominationPoolsUnbondValidationFailure(),
        CrossMinimumBalanceValidationFailure

    class PoolUnlockChunksLimitReached(val timeTillNextAvailableSlot: Duration) : NominationPoolsUnbondValidationFailure()

    class PoolMemberMaxUnlockingLimitReached(val limit: Int) : NominationPoolsUnbondValidationFailure()
}
