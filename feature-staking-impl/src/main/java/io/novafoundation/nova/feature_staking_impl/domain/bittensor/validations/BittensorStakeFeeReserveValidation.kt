package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.maximumBittensorStake
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount

class BittensorStakeFeeReserveValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val networkFee = value.fee.maxAmountDeductionFor(value.asset.token.configuration)
        val maximumStake = maximumBittensorStake(value.asset.transferableInPlanks, networkFee)
        return validOrError(value.asset.token.planksFromAmount(value.amount) <= maximumStake) { BittensorStakeValidationFailure.FeeReserve(maximumStake) }
    }
}
