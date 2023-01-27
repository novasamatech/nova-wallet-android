package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseTrack.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class RemoveVotesValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val availableToPayFees: BigDecimal,
        override val fee: BigDecimal
    ) : RemoveVotesValidationFailure(), NotEnoughToPayFeesError
}
