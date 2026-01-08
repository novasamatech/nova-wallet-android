package io.novafoundation.nova.feature_staking_impl.domain.validations.bond

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class BondMoreValidationFailure {

    object NotEnoughToPayFees : BondMoreValidationFailure()

    object NotEnoughStakeable : BondMoreValidationFailure()

    object ZeroBond : BondMoreValidationFailure()

    class NotEnoughFundToStayAboveED(
        override val asset: Chain.Asset,
        override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel
    ) : BondMoreValidationFailure(), InsufficientBalanceToStayAboveEDError
}
