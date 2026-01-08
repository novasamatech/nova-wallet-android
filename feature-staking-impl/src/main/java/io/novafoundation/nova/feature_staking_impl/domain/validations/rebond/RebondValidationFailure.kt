package io.novafoundation.nova.feature_staking_impl.domain.validations.rebond

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed interface RebondValidationFailure {

    object NotEnoughUnbondings : RebondValidationFailure

    object CannotPayFee : RebondValidationFailure

    object ZeroAmount : RebondValidationFailure

    class NotEnoughBalanceToStayAboveED(
        override val asset: Chain.Asset,
        override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel
    ) : RebondValidationFailure, InsufficientBalanceToStayAboveEDError
}
