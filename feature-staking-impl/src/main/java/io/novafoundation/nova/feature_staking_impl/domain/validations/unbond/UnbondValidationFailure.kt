package io.novafoundation.nova.feature_staking_impl.domain.validations.unbond

import io.novafoundation.nova.feature_wallet_api.domain.validation.CrossMinimumBalanceValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.CrossMinimumBalanceValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class UnbondValidationFailure {

    object CannotPayFees : UnbondValidationFailure()

    object NotEnoughBonded : UnbondValidationFailure()

    object ZeroUnbond : UnbondValidationFailure()

    class BondedWillCrossExistential(override val errorContext: CrossMinimumBalanceValidation.ErrorContext) :
        UnbondValidationFailure(),
        CrossMinimumBalanceValidationFailure

    class UnbondLimitReached(val limit: Int) : UnbondValidationFailure()

    class NotEnoughBalanceToStayAboveED(override val asset: Chain.Asset, override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel) :
        UnbondValidationFailure(),
        InsufficientBalanceToStayAboveEDError
}
