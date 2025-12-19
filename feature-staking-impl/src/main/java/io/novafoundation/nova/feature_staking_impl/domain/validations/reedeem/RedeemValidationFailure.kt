package io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed interface RedeemValidationFailure {
    object CannotPayFees : RedeemValidationFailure

    class NotEnoughBalanceToStayAboveED(
        override val asset: Chain.Asset,
        override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel
    ) : RedeemValidationFailure, InsufficientBalanceToStayAboveEDError
}
