package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class SetupStakingValidationFailure {

    object CannotPayFee : SetupStakingValidationFailure()

    object NotEnoughStakeable : SetupStakingValidationFailure()

    class AmountLessThanMinimum(override val context: StakingMinimumBondError.Context) : SetupStakingValidationFailure(), StakingMinimumBondError

    object MaxNominatorsReached : SetupStakingValidationFailure()

    class NotEnoughFundToStayAboveED(
        override val asset: Chain.Asset,
        override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel
    ) : SetupStakingValidationFailure(), InsufficientBalanceToStayAboveEDError
}
