package io.novafoundation.nova.feature_staking_impl.domain.validations.unbond

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks

class CrossExistentialValidation(
    private val walletConstants: WalletConstants,
) : UnbondValidation {

    override suspend fun validate(value: UnbondValidationPayload): ValidationStatus<UnbondValidationFailure> {
        val tokenConfiguration = value.asset.token.configuration

        val existentialDepositInPlanks = walletConstants.existentialDeposit(tokenConfiguration.chainId)
        val existentialDeposit = tokenConfiguration.amountFromPlanks(existentialDepositInPlanks)

        val bonded = value.asset.bonded
        val resultGreaterThanExistential = bonded - value.amount >= existentialDeposit
        val resultIsZero = bonded == value.amount

        return validOrWarning(resultGreaterThanExistential || resultIsZero) {
            UnbondValidationFailure.BondedWillCrossExistential(willBeUnbonded = bonded)
        }
    }
}
