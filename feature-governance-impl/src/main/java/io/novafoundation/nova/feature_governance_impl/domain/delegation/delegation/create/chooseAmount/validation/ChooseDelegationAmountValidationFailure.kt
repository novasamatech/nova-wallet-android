package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughFreeBalanceError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class ChooseDelegationAmountValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : ChooseDelegationAmountValidationFailure(), NotEnoughToPayFeesError

    object CannotDelegateToSelf : ChooseDelegationAmountValidationFailure()

    class AmountIsTooBig(
        override val chainAsset: Chain.Asset,
        override val freeAfterFees: BigDecimal,
    ) : ChooseDelegationAmountValidationFailure(), NotEnoughFreeBalanceError
}
