package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.NotEnoughToBond
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.NotPositiveAmount
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationFailure.PoolIsDestroying
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount

fun nominationPoolsBondMoreValidationFailure(
    failure: NominationPoolsBondMoreValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (failure) {
        is NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(failure, resourceManager)

        NotEnoughToBond -> resourceManager.amountIsTooBig()

        NotPositiveAmount -> resourceManager.zeroAmount()

        PoolIsDestroying -> resourceManager.getString(R.string.nomination_pools_pool_destroying_error_title) to
            resourceManager.getString(R.string.nomination_pools_pool_destroying_error_message)
    }
}
