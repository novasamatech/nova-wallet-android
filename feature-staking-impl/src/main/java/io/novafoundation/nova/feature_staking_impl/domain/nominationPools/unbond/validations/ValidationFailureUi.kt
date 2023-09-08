package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.NotEnoughToUnbond
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.NotPositiveAmount
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PartialUnbondLeavesLessThanMinBond
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PoolMemberMaxUnlockingLimitReached
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationFailure.PoolUnlockChunksLimitReached
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleWith
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount

fun nominationPoolsUnbondValidationFailure(
    status: ValidationStatus.NotValid<NominationPoolsUnbondValidationFailure>,
    flowActions: ValidationFlowActions<NominationPoolsUnbondValidationPayload>,
    resourceManager: ResourceManager
): TransformedFailure {
    return when (val failure = status.reason) {
        is NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(failure, resourceManager).asDefault()

        NotEnoughToUnbond -> resourceManager.amountIsTooBig().asDefault()

        NotPositiveAmount -> resourceManager.zeroAmount().asDefault()

        is PartialUnbondLeavesLessThanMinBond -> failure.handleWith(resourceManager, flowActions) { oldPayload, newAmount ->
            oldPayload.copy(amount = newAmount)
        }

        is PoolMemberMaxUnlockingLimitReached ->
            TransformedFailure.Default(
                resourceManager.getString(R.string.staking_unbonding_limit_reached_title) to
                    resourceManager.getString(R.string.staking_unbonding_limit_reached_message, failure.limit)
            )

        is PoolUnlockChunksLimitReached -> {
            val durationFormatted = resourceManager.formatDuration(failure.timeTillNextAvailableSlot)

            TransformedFailure.Default(
                resourceManager.getString(R.string.nomination_pools_pool_reached_unbondings_limit_title) to
                    resourceManager.getString(R.string.nomination_pools_pool_reached_unbondings_limit_message, durationFormatted)
            )
        }
    }
}
