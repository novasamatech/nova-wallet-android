package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.NotEnoughToUnbond
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.NotPositiveAmount
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PartialUnbondLeavesLessThanMinBond
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PoolMemberMaxUnlockingLimitReached
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PoolUnlockChunksLimitReached
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.formatWith
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount

fun nominationPoolsUnbondValidationFailure(
    failure: NominationPoolsUnbondValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when(failure){
        is NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(failure, resourceManager)

        NotEnoughToUnbond ->  resourceManager.amountIsTooBig()

        NotPositiveAmount ->  resourceManager.zeroAmount()

        is PartialUnbondLeavesLessThanMinBond -> failure.formatWith(resourceManager)

        is PoolMemberMaxUnlockingLimitReached -> resourceManager.getString(R.string.staking_unbonding_limit_reached_title) to
            resourceManager.getString(R.string.staking_unbonding_limit_reached_message, failure.limit)

        is PoolUnlockChunksLimitReached -> {
            val durationFormatted = resourceManager.formatDuration(failure.timeTillNextAvailableSlot)

            resourceManager.getString(R.string.nomination_pools_pool_reached_unbondings_limit_title) to
                resourceManager.getString(R.string.nomination_pools_pool_reached_unbondings_limit_message, durationFormatted)
        }
    }
}


fun nominationPoolsUnbondValidationPayloadAutoFix(
    payload: NominationPoolsUnbondValidationPayload,
    reason: NominationPoolsUnbondValidationFailure
): NominationPoolsUnbondValidationPayload {
    return when (reason) {
        is PartialUnbondLeavesLessThanMinBond -> payload.copy(amount = reason.errorContext.wholeAmount)
        else -> payload
    }
}
