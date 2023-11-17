package io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class RebagValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : RebagValidationFailure(), NotEnoughToPayFeesError
}
