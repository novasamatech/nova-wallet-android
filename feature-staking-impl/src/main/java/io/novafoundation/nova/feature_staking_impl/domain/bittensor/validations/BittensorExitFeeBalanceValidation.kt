package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot

class BittensorExitFeeBalanceValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        if (!value.feeFromStake) return valid()

        val quote = value.quote
        val canPayFeeFromStake = quote.netuid.isRoot() && value.supportsFeeFromStake && quote.exitHotkeys.size <= 1 && quote.plan?.isBatched != true
        val withdrawalCoversFeeAndDeposit = quote.amount > value.fee + value.existentialDeposit
        val stakeCoversWithdrawalAndFee = quote.amount <= value.preflight.totalStake

        return validOrError(canPayFeeFromStake && withdrawalCoversFeeAndDeposit && stakeCoversWithdrawalAndFee) {
            BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE
        }
    }
}
