package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import java.math.BigInteger

class BittensorMinimumStakeValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val amount = value.asset.token.planksFromAmount(value.amount)
        val serviceFee = value.quote?.serviceFeeTaoInPlanks ?: BigInteger.ZERO
        val poolFee = value.quote?.simulation?.taoFee ?: BigInteger.ZERO
        return validOrError(amount - serviceFee >= value.preflight.minimumStake + poolFee) { BittensorStakeValidationFailure.BELOW_MINIMUM_STAKE }
    }
}
