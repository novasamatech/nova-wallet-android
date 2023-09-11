package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolAvailableBalanceValidation

sealed class NominationPoolsBondMoreValidationFailure {

    class NotEnoughToBond(
        override val context: PoolAvailableBalanceValidation.ValidationError.Context
    ) : PoolAvailableBalanceValidation.ValidationError, NominationPoolsBondMoreValidationFailure()

    object NotPositiveAmount : NominationPoolsBondMoreValidationFailure()

    object PoolIsDestroying : NominationPoolsBondMoreValidationFailure()

    object UnstakingAll : NominationPoolsBondMoreValidationFailure()
}
