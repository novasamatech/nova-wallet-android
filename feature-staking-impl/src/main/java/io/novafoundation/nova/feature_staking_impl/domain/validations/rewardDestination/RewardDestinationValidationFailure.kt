package io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination

import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class RewardDestinationValidationFailure {
    object CannotPayFees : RewardDestinationValidationFailure()

    class MissingController(val controllerAddress: String) : RewardDestinationValidationFailure()

    class NotEnoughBalanceToStayAboveED(override val asset: Chain.Asset, override val errorModel: InsufficientBalanceToStayAboveEDError.ErrorModel) :
        RewardDestinationValidationFailure(),
        InsufficientBalanceToStayAboveEDError
}
