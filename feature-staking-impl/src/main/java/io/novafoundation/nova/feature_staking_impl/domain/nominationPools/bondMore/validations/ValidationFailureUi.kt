package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.NotEnoughToBond
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.NotPositiveAmount
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.PoolIsDestroying
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.StakingTypesConflict
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.UnstakingAll
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.handlePoolAvailableBalanceError
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.handlePoolStakingTypesConflictValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount
import java.math.BigDecimal

fun nominationPoolsBondMoreValidationFailure(
    validationStatus: ValidationStatus.NotValid<NominationPoolsBondMoreValidationFailure>,
    resourceManager: ResourceManager,
    flowActions: ValidationFlowActions<NominationPoolsBondMoreValidationPayload>,
    updateAmountInUi: (maxAmountToStake: BigDecimal) -> Unit = {}
): TransformedFailure {
    return when (val reason = validationStatus.reason) {
        is NotEnoughToBond -> handlePoolAvailableBalanceError(
            error = reason,
            resourceManager = resourceManager,
            flowActions = flowActions,
            modifyPayload = { oldPayload, maxAmountToStake -> oldPayload.copy(amount = maxAmountToStake) },
            updateAmountInUi = updateAmountInUi
        )

        NotPositiveAmount -> TransformedFailure.Default(resourceManager.zeroAmount())

        PoolIsDestroying -> TransformedFailure.Default(
            resourceManager.getString(R.string.nomination_pools_pool_destroying_error_title) to
                resourceManager.getString(R.string.nomination_pools_pool_destroying_error_message)
        )

        UnstakingAll -> TransformedFailure.Default(
            resourceManager.getString(R.string.staking_unable_to_stake_more_title) to
                resourceManager.getString(R.string.staking_unable_to_stake_more_message)
        )

        StakingTypesConflict -> TransformedFailure.Default(handlePoolStakingTypesConflictValidationFailure(resourceManager))
    }
}
